package com.smartlb.customerservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoadBalancerConfigResponse {
    private UUID id;
    private UUID websiteId;
    private String algorithm;
    private Boolean stickySessions;
    private Integer sessionTimeout;
    private Integer healthCheckInterval;
    private Integer requestTimeout;
    private Integer retryAttempts;
    private Instant createdAt;
    private Instant updatedAt;
}
