package com.smartlb.loadbalancerservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing an application server instance managed by SmartLB-AI.
 */
@Entity
@Table(name = "application_instances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationInstance extends BaseEntity {

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "host", nullable = false)
    private String host;

    @Column(name = "port", nullable = false)
    private Integer port;

    @Column(name = "base_url", nullable = false)
    private String baseUrl;

    @Column(name = "health_check_endpoint", nullable = false)
    @Builder.Default
    private String healthCheckEndpoint = "/health";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private InstanceStatus status = InstanceStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "health_status", nullable = false)
    @Builder.Default
    private HealthStatus healthStatus = HealthStatus.UNKNOWN;

    @Column(name = "weight", nullable = false)
    @Builder.Default
    private Integer weight = 1;

    @Column(name = "current_connections", nullable = false)
    @Builder.Default
    private Integer currentConnections = 0;

    @Column(name = "consecutive_failures", nullable = false)
    @Builder.Default
    private Integer consecutiveFailures = 0;

    @Column(name = "last_health_check")
    private Instant lastHealthCheck;
}
