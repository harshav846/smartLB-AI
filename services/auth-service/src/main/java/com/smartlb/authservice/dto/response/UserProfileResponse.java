package com.smartlb.authservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.UUID;

/**
 * Data representation of the user profile context returned upon authorization.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private UUID userId;
    private UUID organizationId;
    private String organizationName;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String profileImage;
    private List<String> roles;
    private boolean emailVerified;
}
