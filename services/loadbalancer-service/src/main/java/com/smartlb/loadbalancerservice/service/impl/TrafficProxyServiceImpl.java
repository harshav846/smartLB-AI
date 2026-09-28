package com.smartlb.loadbalancerservice.service.impl;

import com.smartlb.loadbalancerservice.algorithm.ClientRequestMetadata;
import com.smartlb.loadbalancerservice.algorithm.LoadBalancingAlgorithm;
import com.smartlb.loadbalancerservice.algorithm.LoadBalancingStrategy;
import com.smartlb.loadbalancerservice.algorithm.LoadBalancingStrategyFactory;
import com.smartlb.loadbalancerservice.dto.response.ProxyResponse;
import com.smartlb.loadbalancerservice.entity.ApplicationInstance;
import com.smartlb.loadbalancerservice.entity.HealthStatus;
import com.smartlb.loadbalancerservice.exception.ResourceNotFoundException;
import com.smartlb.loadbalancerservice.repository.ApplicationInstanceRepository;
import com.smartlb.loadbalancerservice.service.TrafficProxyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of {@link TrafficProxyService}.
 * <p>
 * Flow:
 * 1. Fetch all ACTIVE+HEALTHY instances for the tenant.
 * 2. Select one using the requested {@link LoadBalancingAlgorithm}.
 * 3. Increment the instance's active-connection counter.
 * 4. Forward the HTTP request using Spring {@link RestTemplate}.
 * 5. Decrement the counter and record latency in the response.
 * If the first attempt fails with a connection error, one automatic retry
 * is made against a different (next) instance.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrafficProxyServiceImpl implements TrafficProxyService {

    /* ── constants ─────────────────────────────────────────────────────── */
    private static final String[] FORWARDED_HEADERS_BLACKLIST = {
            "host", "content-length", "transfer-encoding", "connection"
    };

    /* ── dependencies ──────────────────────────────────────────────────── */
    private final ApplicationInstanceRepository instanceRepository;
    private final LoadBalancingStrategyFactory strategyFactory;
    private final RestTemplate restTemplate;

    @Value("${app.proxy.connect-timeout-ms:5000}")
    private int connectTimeoutMs;

    @Value("${app.proxy.max-retries:1}")
    private int maxRetries;

    /* ═══════════════════════════════════════════════════════════════════ */
    /*  Public API                                                         */
    /* ═══════════════════════════════════════════════════════════════════ */

    @Override
    @Transactional
    public ProxyResponse proxy(UUID organizationId,
                               String method,
                               String path,
                               String body,
                               Map<String, String> headers,
                               String clientIp,
                               LoadBalancingAlgorithm algorithm) {

        List<ApplicationInstance> candidates = resolveHealthyCandidates(organizationId);

        LoadBalancingStrategy strategy = strategyFactory.getStrategy(algorithm);
        ClientRequestMetadata metadata = buildMetadata(organizationId, clientIp, method, path, headers);

        ApplicationInstance selected = strategy.selectInstance(candidates, metadata);
        log.info("[PROXY] org={} algo={} selected={} ({}:{})",
                organizationId, strategy.getAlgorithm(), selected.getId(),
                selected.getHost(), selected.getPort());

        return executeWithRetry(selected, candidates, metadata, strategy, method, path, body, headers, algorithm);
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationInstance selectInstance(UUID organizationId,
                                             String clientIp,
                                             LoadBalancingAlgorithm algorithm) {

        List<ApplicationInstance> candidates = resolveHealthyCandidates(organizationId);
        LoadBalancingStrategy strategy = strategyFactory.getStrategy(algorithm);
        ClientRequestMetadata metadata = buildMetadata(organizationId, clientIp, "GET", "/", Map.of());
        return strategy.selectInstance(candidates, metadata);
    }

    /* ═══════════════════════════════════════════════════════════════════ */
    /*  Private helpers                                                     */
    /* ═══════════════════════════════════════════════════════════════════ */

    private List<ApplicationInstance> resolveHealthyCandidates(UUID organizationId) {
        List<ApplicationInstance> candidates = instanceRepository
                .findHealthyInstancesByOrganizationId(organizationId);

        if (candidates.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No healthy instances available for organization " + organizationId);
        }
        return candidates;
    }

    private ClientRequestMetadata buildMetadata(UUID orgId, String clientIp,
                                                String method, String path,
                                                Map<String, String> headers) {
        return ClientRequestMetadata.builder()
                .organizationId(orgId)
                .clientIp(clientIp != null ? clientIp : "unknown")
                .method(method)
                .path(path)
                .headers(headers != null ? headers : Map.of())
                .build();
    }

    /**
     * Forwards the request and retries once with a different instance on connection failure.
     */
    private ProxyResponse executeWithRetry(ApplicationInstance primary,
                                           List<ApplicationInstance> allCandidates,
                                           ClientRequestMetadata metadata,
                                           LoadBalancingStrategy strategy,
                                           String method, String path, String body,
                                           Map<String, String> headers,
                                           LoadBalancingAlgorithm algorithm) {
        try {
            return forward(primary, method, path, body, headers, algorithm, false);
        } catch (ResourceAccessException connectError) {
            log.warn("[PROXY] Connection failure on primary instance {} — attempting retry. Error: {}",
                    primary.getId(), connectError.getMessage());

            // Mark primary as possibly failing, select next from remaining
            List<ApplicationInstance> fallback = allCandidates.stream()
                    .filter(i -> !i.getId().equals(primary.getId()))
                    .toList();

            if (fallback.isEmpty()) {
                log.error("[PROXY] No fallback instances available for organization {}", primary.getOrganizationId());
                throw connectError; // re-throw to surface 503
            }

            ApplicationInstance retry = strategy.selectInstance(fallback, metadata);
            log.info("[PROXY] Retry on fallback instance {}", retry.getId());
            return forward(retry, method, path, body, headers, algorithm, true);
        }
    }

    /**
     * Performs a single HTTP forward to the given instance.
     */
    private ProxyResponse forward(ApplicationInstance instance,
                                  String method,
                                  String path,
                                  String body,
                                  Map<String, String> headers,
                                  LoadBalancingAlgorithm algorithm,
                                  boolean retried) {

        String targetUrl = buildTargetUrl(instance, path);

        HttpHeaders forwardHeaders = buildForwardHeaders(headers);
        HttpEntity<String> requestEntity = new HttpEntity<>(body, forwardHeaders);

        incrementConnections(instance);
        long start = System.currentTimeMillis();
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    targetUrl,
                    HttpMethod.valueOf(method.toUpperCase()),
                    requestEntity,
                    String.class
            );

            long latency = System.currentTimeMillis() - start;
            log.info("[PROXY] Response from {} → status={} latency={}ms", instance.getId(),
                    response.getStatusCode().value(), latency);

            return ProxyResponse.builder()
                    .instanceId(instance.getId())
                    .instanceName(instance.getName())
                    .targetUrl(targetUrl)
                    .httpStatus(response.getStatusCode().value())
                    .responseBody(response.getBody())
                    .latencyMs(latency)
                    .algorithm(algorithm)
                    .retried(retried)
                    .build();

        } catch (HttpStatusCodeException httpEx) {
            long latency = System.currentTimeMillis() - start;
            // Still a valid HTTP response from backend — surface it to caller
            return ProxyResponse.builder()
                    .instanceId(instance.getId())
                    .instanceName(instance.getName())
                    .targetUrl(targetUrl)
                    .httpStatus(httpEx.getStatusCode().value())
                    .responseBody(httpEx.getResponseBodyAsString())
                    .latencyMs(latency)
                    .algorithm(algorithm)
                    .retried(retried)
                    .build();
        } finally {
            decrementConnections(instance);
        }
    }

    private String buildTargetUrl(ApplicationInstance instance, String path) {
        String base = instance.getBaseUrl();
        if (base.endsWith("/") && path.startsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + path;
    }

    private HttpHeaders buildForwardHeaders(Map<String, String> incomingHeaders) {
        HttpHeaders headers = new HttpHeaders();
        if (incomingHeaders == null) return headers;

        incomingHeaders.forEach((key, value) -> {
            if (shouldForwardHeader(key)) {
                headers.add(key, value);
            }
        });
        return headers;
    }

    private boolean shouldForwardHeader(String headerName) {
        if (headerName == null) return false;
        String lower = headerName.toLowerCase();
        for (String blocked : FORWARDED_HEADERS_BLACKLIST) {
            if (lower.equals(blocked)) return false;
        }
        return true;
    }

    @Transactional
    protected void incrementConnections(ApplicationInstance instance) {
        instance.setCurrentConnections(instance.getCurrentConnections() + 1);
        instanceRepository.save(instance);
    }

    @Transactional
    protected void decrementConnections(ApplicationInstance instance) {
        int current = instance.getCurrentConnections();
        instance.setCurrentConnections(Math.max(0, current - 1));
        instanceRepository.save(instance);
    }
}
