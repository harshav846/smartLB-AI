package com.smartlb.loadbalancerservice.dto.response;

import com.smartlb.loadbalancerservice.algorithm.LoadBalancingAlgorithm;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * Response returned from a proxied request to a backend instance.
 */
@Getter
@Builder
public class ProxyResponse {
    private final UUID instanceId;
    private final String instanceName;
    private final String targetUrl;
    private final int httpStatus;
    private final String responseBody;
    private final long latencyMs;
    private final LoadBalancingAlgorithm algorithm;
    private final boolean retried;
}
