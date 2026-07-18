package com.smartlb.authservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

/**
 * Response returned after registering an Organization and owner.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterOrganizationResponse {

    private UUID organizationId;
    private String organizationSlug;
    private UUID adminId;
    private String adminEmail;
    private String message;
}
