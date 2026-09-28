package com.smartlb.authservice.dto.request;

import com.smartlb.authservice.validation.annotation.ValidOrganizationName;
import com.smartlb.authservice.validation.annotation.ValidOrganizationSlug;
import com.smartlb.authservice.validation.annotation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for creating a tenant Organization and its first ORG_ADMIN user.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterOrganizationRequest {

    @NotBlank(message = "Organization name is required")
    @ValidOrganizationName
    private String organizationName;

    @NotBlank(message = "Organization slug is required")
    @Size(max = 255, message = "Slug cannot exceed 255 characters")
    @ValidOrganizationSlug
    private String organizationSlug;

    @Email(message = "Invalid company email format")
    @Size(max = 255, message = "Company email cannot exceed 255 characters")
    private String companyEmail;

    @Size(max = 255, message = "Website URI cannot exceed 255 characters")
    private String website;

    @Size(max = 100, message = "Country name cannot exceed 100 characters")
    private String country;

    @Size(max = 100, message = "Timezone string cannot exceed 100 characters")
    private String timezone;

    @NotBlank(message = "Admin first name is required")
    @Size(max = 50, message = "First name cannot exceed 50 characters")
    private String adminFirstName;

    @NotBlank(message = "Admin last name is required")
    @Size(max = 50, message = "Last name cannot exceed 50 characters")
    private String adminLastName;

    @NotBlank(message = "Admin email is required")
    @Email(message = "Invalid admin email format")
    @Size(max = 255, message = "Admin email cannot exceed 255 characters")
    private String adminEmail;

    @NotBlank(message = "Admin password is required")
    @ValidPassword
    private String adminPassword;

    @Size(max = 50, message = "Phone number cannot exceed 50 characters")
    private String phone;
}
