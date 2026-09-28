package com.smartlb.loadbalancerservice.service;

import com.smartlb.loadbalancerservice.dto.response.InstanceHealthResponse;

import java.util.UUID;

/**
 * Interface contract for periodic and manual instance health monitoring checks.
 */
public interface HealthMonitoringService {

    /**
     * Executes health checks for all ACTIVE application instances across tenants.
     */
    void performScheduledHealthChecks();

    /**
     * Evaluates health for a specific instance within an organization scope.
     *
     * @param organizationId tenant organization UUID
     * @param instanceId     application instance UUID
     * @return health snapshot DTO
     */
    InstanceHealthResponse getSingleInstanceHealth(UUID organizationId, UUID instanceId);
}
