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
public class WebsiteResponse {
    private UUID id;
    private UUID organizationId;
    private String domainName;
    private String displayName;
    private String description;
    private String environment;
    private String status;
    private String verificationStatus;
    private Instant createdAt;
    private Instant updatedAt;
}
