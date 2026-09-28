package com.smartlb.customerservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "backend_servers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BackendServer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "website_id", nullable = false)
    private UUID websiteId;

    @Column(name = "server_name", nullable = false)
    private String serverName;

    @Column(name = "private_ip", nullable = false, length = 45)
    private String privateIp;

    @Column(name = "public_ip", length = 45)
    private String publicIp;

    @Column(name = "port", nullable = false)
    private Integer port;

    @Column(name = "protocol", nullable = false, length = 10)
    @Builder.Default
    private String protocol = "HTTP";

    @Column(name = "weight", nullable = false)
    @Builder.Default
    private Integer weight = 1;

    @Column(name = "priority", nullable = false)
    @Builder.Default
    private Integer priority = 1;

    @Column(name = "max_connections", nullable = false)
    @Builder.Default
    private Integer maxConnections = 1000;

    @Column(name = "current_connections", nullable = false)
    @Builder.Default
    private Integer currentConnections = 0;

    @Column(name = "cpu_usage", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal cpuUsage = BigDecimal.ZERO;

    @Column(name = "memory_usage", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal memoryUsage = BigDecimal.ZERO;

    @Column(name = "disk_usage", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal diskUsage = BigDecimal.ZERO;

    @Column(name = "health_status", nullable = false, length = 50)
    @Builder.Default
    private String healthStatus = "HEALTHY";

    @Column(name = "server_status", nullable = false, length = 50)
    @Builder.Default
    private String serverStatus = "ONLINE";

    @Column(name = "response_time_ms", nullable = false)
    @Builder.Default
    private Integer responseTimeMs = 0;

    @Column(name = "last_health_check")
    private Instant lastHealthCheck;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;
}
