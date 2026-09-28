package com.smartlb.customerservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "load_balancer_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoadBalancerConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "website_id", nullable = false, unique = true)
    private UUID websiteId;

    @Column(name = "algorithm", nullable = false, length = 50)
    @Builder.Default
    private String algorithm = "ROUND_ROBIN";

    @Column(name = "sticky_sessions", nullable = false)
    @Builder.Default
    private Boolean stickySessions = false;

    @Column(name = "session_timeout", nullable = false)
    @Builder.Default
    private Integer sessionTimeout = 3600;

    @Column(name = "health_check_interval", nullable = false)
    @Builder.Default
    private Integer healthCheckInterval = 30;

    @Column(name = "request_timeout", nullable = false)
    @Builder.Default
    private Integer requestTimeout = 15;

    @Column(name = "retry_attempts", nullable = false)
    @Builder.Default
    private Integer retryAttempts = 3;

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
