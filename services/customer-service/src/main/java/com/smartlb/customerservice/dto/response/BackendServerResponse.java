package com.smartlb.customerservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackendServerResponse {
    private UUID id;
    private UUID websiteId;
    private String serverName;
    private String privateIp;
    private String publicIp;
    private Integer port;
    private String protocol;
    private Integer weight;
    private Integer priority;
    private Integer maxConnections;
    private Integer currentConnections;
    private BigDecimal cpuUsage;
    private BigDecimal memoryUsage;
    private BigDecimal diskUsage;
    private String healthStatus;
    private String serverStatus;
    private Integer responseTimeMs;
    private Instant lastHealthCheck;
    private Instant createdAt;
    private Instant updatedAt;
}
