package com.smartlb.loadbalancerservice.entity;

/**
 * Health evaluation state of an application server instance.
 */
public enum HealthStatus {
    HEALTHY,
    UNHEALTHY,
    UNKNOWN
}
