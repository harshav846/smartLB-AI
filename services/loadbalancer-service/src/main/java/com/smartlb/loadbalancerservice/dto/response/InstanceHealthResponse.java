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
 * Health status summary response for an application server instance.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstanceHealthResponse {

    private UUID instanceId;
    private String name;
    private String baseUrl;
    private InstanceStatus status;
    private HealthStatus healthStatus;
    private Integer consecutiveFailures;
    private Instant lastHealthCheck;
}
