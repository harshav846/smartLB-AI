package com.smartlb.loadbalancerservice.entity;

/**
 * Operational lifecycle status of an application server instance.
 */
public enum InstanceStatus {
    ACTIVE,
    INACTIVE,
    DRAINING,
    DECOMMISSIONED
}
