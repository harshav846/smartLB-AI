package com.smartlb.loadbalancerservice.service;

import com.smartlb.loadbalancerservice.algorithm.LoadBalancingAlgorithm;
import com.smartlb.loadbalancerservice.dto.response.ProxyResponse;
import com.smartlb.loadbalancerservice.entity.ApplicationInstance;

import java.util.Map;
import java.util.UUID;

/**
 * Core traffic-proxy service for SmartLB-AI.
 * Selects a backend instance using the specified load balancing algorithm
 * and forwards the incoming HTTP request to it.
 */
public interface TrafficProxyService {

    /**
     * Select a backend instance and forward the request to it.
     *
     * @param organizationId tenant ID extracted from the JWT
     * @param method         HTTP method (GET, POST, etc.)
     * @param path           request path + query, e.g. "/api/v1/foo?bar=1"
     * @param body           optional request body
     * @param headers        request headers to forward
     * @param clientIp       original client IP (used by IP_HASH algorithm)
     * @param algorithm      preferred algorithm; null → ROUND_ROBIN
     * @return a ProxyResponse describing the selected instance and upstream response
     */
    ProxyResponse proxy(
            UUID organizationId,
            String method,
            String path,
            String body,
            Map<String, String> headers,
            String clientIp,
            LoadBalancingAlgorithm algorithm
    );

    /**
     * Choose a backend instance without actually sending the request.
     * Useful for dry-run / debugging endpoints.
     *
     * @param organizationId tenant ID
     * @param clientIp       client IP for IP_HASH
     * @param algorithm      preferred algorithm
     * @return the selected ApplicationInstance
     */
    ApplicationInstance selectInstance(UUID organizationId, String clientIp, LoadBalancingAlgorithm algorithm);
}
