package com.smartlb.loadbalancerservice.dto.response;

import com.smartlb.loadbalancerservice.entity.HealthStatus;
import com.smartlb.loadbalancerservice.entity.InstanceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Detailed DTO representation of an application server instance.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstanceResponse {

    private UUID id;
    private UUID organizationId;
    private String name;
    private String host;
    private Integer port;
    private String baseUrl;
    private String healthCheckEndpoint;
    private InstanceStatus status;
    private HealthStatus healthStatus;
    private Integer weight;
    private Integer currentConnections;
    private Integer consecutiveFailures;
    private Instant lastHealthCheck;
    private Instant createdAt;
    private Instant updatedAt;
}
