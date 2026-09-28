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
public class RoutingRuleResponse {
    private UUID id;
    private UUID websiteId;
    private String pathPattern;
    private UUID targetServer;
    private Integer priority;
    private String ruleType;
    private Boolean enabled;
    private Instant createdAt;
    private Instant updatedAt;
}
