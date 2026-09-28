package com.smartlb.loadbalancerservice.algorithm;

/**
 * Enumeration of supported load balancing algorithms in SmartLB-AI.
 */
public enum LoadBalancingAlgorithm {
    ROUND_ROBIN,
    WEIGHTED_ROUND_ROBIN,
    LEAST_CONNECTIONS,
    IP_HASH,
    AI_ASSISTED;

    public static LoadBalancingAlgorithm fromString(String name) {
        if (name == null || name.isBlank()) {
            return ROUND_ROBIN;
        }
        try {
            return LoadBalancingAlgorithm.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ROUND_ROBIN;
        }
    }
}
