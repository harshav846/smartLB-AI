package com.smartlb.customerservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebsiteRequest {

    @NotBlank(message = "Domain name cannot be empty")
    @Size(max = 255, message = "Domain name cannot exceed 255 characters")
    private String domainName;

    @NotBlank(message = "Display name cannot be empty")
    @Size(max = 255, message = "Display name cannot exceed 255 characters")
    private String displayName;

    private String description;

    @Pattern(regexp = "DEVELOPMENT|STAGING|PRODUCTION", message = "Environment must be DEVELOPMENT, STAGING, or PRODUCTION")
    @Builder.Default
    private String environment = "PRODUCTION";
}
