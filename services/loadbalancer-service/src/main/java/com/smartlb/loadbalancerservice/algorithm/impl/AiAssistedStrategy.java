package com.smartlb.loadbalancerservice.algorithm.impl;

import com.smartlb.loadbalancerservice.algorithm.ClientRequestMetadata;
import com.smartlb.loadbalancerservice.algorithm.LoadBalancingAlgorithm;
import com.smartlb.loadbalancerservice.algorithm.LoadBalancingStrategy;
import com.smartlb.loadbalancerservice.entity.ApplicationInstance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI-Assisted load balancing strategy: invokes the AI service for predictive routing,
 * with mandatory seamless fallback to Least Connections upon timeout or service failure.
 */
@Slf4j
@Component
public class AiAssistedStrategy implements LoadBalancingStrategy {

    private final String aiServiceUrl;
    private final RestTemplate restTemplate;
    private final LeastConnectionsStrategy fallbackStrategy;

    public AiAssistedStrategy(
            @Value("${app.ai-service.url:http://localhost:8000}") String aiServiceUrl,
            @Value("${app.ai-service.timeout-ms:800}") int timeoutMs,
            LeastConnectionsStrategy fallbackStrategy) {
        this.aiServiceUrl = aiServiceUrl;
        this.fallbackStrategy = fallbackStrategy;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public ApplicationInstance selectInstance(List<ApplicationInstance> instances, ClientRequestMetadata metadata) {
        if (instances == null || instances.isEmpty()) {
            return null;
        }

        try {
            Map<String, Object> requestPayload = new HashMap<>();
            requestPayload.put("client_ip", metadata != null && metadata.getClientIp() != null ? metadata.getClientIp() : "127.0.0.1");
            requestPayload.put("request_path", metadata != null && metadata.getPath() != null ? metadata.getPath() : "/");

            List<Map<String, Object>> serverStats = new ArrayList<>();
            for (ApplicationInstance instance : instances) {
                Map<String, Object> stat = new HashMap<>();
                stat.put("server_id", instance.getId().toString());
                stat.put("active_connections", instance.getCurrentConnections() != null ? instance.getCurrentConnections() : 0);
                stat.put("cpu_usage", 0.0);
                stat.put("error_rate", instance.getConsecutiveFailures() != null ? instance.getConsecutiveFailures() * 1.0 : 0.0);
                stat.put("latency_ms", 10.0);
                serverStats.add(stat);
            }
            requestPayload.put("servers", serverStats);

            String url = aiServiceUrl.replaceAll("/+$", "") + "/routing/decision";
            ResponseEntity<Map> response = restTemplate.postForEntity(url, requestPayload, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object recObj = response.getBody().get("recommended_server_id");
                if (recObj != null) {
                    String recommendedId = recObj.toString();
                    for (ApplicationInstance inst : instances) {
                        if (inst.getId().toString().equalsIgnoreCase(recommendedId) || inst.getName().equalsIgnoreCase(recommendedId)) {
                            log.debug("AI routing selected instance '{}' (ID: {})", inst.getName(), inst.getId());
                            return inst;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("AI service consultation failed ({}: {}). Falling back to Least Connections.",
                    ex.getClass().getSimpleName(), ex.getMessage());
        }

        return fallbackStrategy.selectInstance(instances, metadata);
    }

    @Override
    public LoadBalancingAlgorithm getAlgorithm() {
        return LoadBalancingAlgorithm.AI_ASSISTED;
    }
}
