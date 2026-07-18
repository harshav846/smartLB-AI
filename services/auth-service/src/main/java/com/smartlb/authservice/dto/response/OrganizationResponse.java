package com.smartlb.authservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Data representation structure of registered Tenant Organizations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationResponse {

    private UUID id;
    private String name;
    private String slug;
    private String companyEmail;
    private String website;
    private String country;
    private String timezone;
    private String subscriptionType;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
