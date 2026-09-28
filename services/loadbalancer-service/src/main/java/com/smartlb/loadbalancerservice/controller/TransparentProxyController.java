package com.smartlb.loadbalancerservice.controller;

import com.smartlb.loadbalancerservice.algorithm.ClientRequestMetadata;
import com.smartlb.loadbalancerservice.algorithm.LoadBalancingAlgorithm;
import com.smartlb.loadbalancerservice.algorithm.LoadBalancingStrategy;
import com.smartlb.loadbalancerservice.algorithm.LoadBalancingStrategyFactory;
import com.smartlb.loadbalancerservice.entity.ApplicationInstance;
import com.smartlb.loadbalancerservice.exception.ResourceNotFoundException;
import com.smartlb.loadbalancerservice.repository.ApplicationInstanceRepository;
import com.smartlb.loadbalancerservice.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TransparentProxyController {

    private static final String[] BLACKLIST_HEADERS = {
            "host", "content-length", "transfer-encoding", "connection"
    };

    private final ApplicationInstanceRepository instanceRepository;
    private final LoadBalancingStrategyFactory strategyFactory;
    private final RestTemplate restTemplate;

    @Value("${app.loadbalancer.default-algorithm:ROUND_ROBIN}")
    private String defaultAlgorithm;

    @RequestMapping("/proxy/**")
    public ResponseEntity<byte[]> handleProxy(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) throws IOException {

        // 1. Resolve Organization ID
        UUID organizationId = resolveOrganizationId(principal, request);

        // 2. Resolve candidates
        List<ApplicationInstance> candidates;
        if (organizationId != null) {
            candidates = instanceRepository.findHealthyInstancesByOrganizationId(organizationId);
        } else {
            candidates = instanceRepository.findAllHealthyInstances();
        }

        if (candidates.isEmpty()) {
            String errorMsg = "{\"success\":false,\"message\":\"No healthy backend instances available to handle the request\"}";
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(errorMsg.getBytes(StandardCharsets.UTF_8));
        }

        // 3. Resolve Algorithm
        String algoHeader = request.getHeader("X-LB-Algorithm");
        LoadBalancingAlgorithm algorithm = LoadBalancingAlgorithm.fromString(
                algoHeader != null ? algoHeader : defaultAlgorithm
        );

        // 4. Resolve sub-path
        String fullPath = request.getRequestURI();
        String subPath = fullPath.replaceFirst("^/proxy", "");
        if (subPath.isBlank()) subPath = "/";
        if (request.getQueryString() != null) {
            subPath += "?" + request.getQueryString();
        }

        // 5. Select instance
        String clientIp = resolveClientIp(request);
        Map<String, String> incomingHeaders = extractHeaders(request);
        ClientRequestMetadata metadata = ClientRequestMetadata.builder()
                .organizationId(candidates.get(0).getOrganizationId())
                .clientIp(clientIp)
                .method(request.getMethod())
                .path(subPath)
                .headers(incomingHeaders)
                .build();

        LoadBalancingStrategy strategy = strategyFactory.getStrategy(algorithm);
        ApplicationInstance selected = strategy.selectInstance(candidates, metadata);

        log.info("[TRANSPARENT-PROXY] method={} path={} algo={} selected={} ({}:{})",
                request.getMethod(), subPath, algorithm, selected.getName(), selected.getHost(), selected.getPort());

        // 6. Forward request
        String targetUrl = buildTargetUrl(selected, subPath);
        HttpHeaders forwardHeaders = new HttpHeaders();
        incomingHeaders.forEach((k, v) -> {
            if (shouldForward(k)) forwardHeaders.add(k, v);
        });
        forwardHeaders.add("X-Forwarded-For", clientIp);
        forwardHeaders.add("X-Proxy-By", "SmartLB-AI");
        forwardHeaders.add("X-Selected-Backend", selected.getName());

        byte[] requestBody = StreamUtils.copyToByteArray(request.getInputStream());
        HttpEntity<byte[]> entity = new HttpEntity<>(requestBody, forwardHeaders);

        try {
            incrementConnections(selected);
            long start = System.currentTimeMillis();
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    targetUrl,
                    HttpMethod.valueOf(request.getMethod()),
                    entity,
                    byte[].class
            );
            long latency = System.currentTimeMillis() - start;

            HttpHeaders responseHeaders = new HttpHeaders();
            response.getHeaders().forEach((k, v) -> {
                if (!k.equalsIgnoreCase("transfer-encoding")) {
                    responseHeaders.put(k, v);
                }
            });
            responseHeaders.add("X-Backend-Server", selected.getName());
            responseHeaders.add("X-Response-Time-Ms", String.valueOf(latency));

            return new ResponseEntity<>(response.getBody(), responseHeaders, response.getStatusCode());

        } catch (HttpStatusCodeException ex) {
            HttpHeaders responseHeaders = new HttpHeaders();
            ex.getResponseHeaders().forEach((k, v) -> {
                if (!k.equalsIgnoreCase("transfer-encoding")) {
                    responseHeaders.put(k, v);
                }
            });
            responseHeaders.add("X-Backend-Server", selected.getName());
            return new ResponseEntity<>(ex.getResponseBodyAsByteArray(), responseHeaders, ex.getStatusCode());
        } catch (Exception ex) {
            log.error("[TRANSPARENT-PROXY] Forward error to {}: {}", targetUrl, ex.getMessage());
            String err = "{\"success\":false,\"message\":\"Backend error: " + ex.getMessage() + "\"}";
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(err.getBytes(StandardCharsets.UTF_8));
        } finally {
            decrementConnections(selected);
        }
    }

    private UUID resolveOrganizationId(UserPrincipal principal, HttpServletRequest request) {
        if (principal != null && principal.getOrganizationId() != null) {
            return principal.getOrganizationId();
        }
        String orgHeader = request.getHeader("X-Organization-Id");
        if (orgHeader != null && !orgHeader.isBlank()) {
            try {
                return UUID.fromString(orgHeader.trim());
            } catch (IllegalArgumentException ignored) {}
        }
        return null;
    }

    private String buildTargetUrl(ApplicationInstance instance, String path) {
        String base = instance.getBaseUrl();
        if (base.endsWith("/") && path.startsWith("/")) {
            return base.substring(0, base.length() - 1) + path;
        }
        return base + path;
    }

    private boolean shouldForward(String headerName) {
        for (String b : BLACKLIST_HEADERS) {
            if (b.equalsIgnoreCase(headerName)) return false;
        }
        return true;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String fwd = request.getHeader("X-Forwarded-For");
        if (fwd != null && !fwd.isBlank()) return fwd.split(",")[0].trim();
        return request.getRemoteAddr();
    }

    private Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> map = new HashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        while (names != null && names.hasMoreElements()) {
            String name = names.nextElement();
            map.put(name, request.getHeader(name));
        }
        return map;
    }

    private void incrementConnections(ApplicationInstance instance) {
        instance.setCurrentConnections(instance.getCurrentConnections() + 1);
        instanceRepository.save(instance);
    }

    private void decrementConnections(ApplicationInstance instance) {
        instance.setCurrentConnections(Math.max(0, instance.getCurrentConnections() - 1));
        instanceRepository.save(instance);
    }
}
