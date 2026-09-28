package com.smartlb.loadbalancerservice.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request payload for registering a new application instance.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterInstanceRequest {

    @NotBlank(message = "Instance name is required")
    @Size(min = 2, max = 100, message = "Instance name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Host is required")
    @Size(max = 255, message = "Host must not exceed 255 characters")
    private String host;

    @NotNull(message = "Port is required")
    @Min(value = 1, message = "Port must be greater than 0")
    @Max(value = 65535, message = "Port must not exceed 65535")
    private Integer port;

    private String baseUrl;

    @Pattern(regexp = "^/.*", message = "Health check endpoint must start with '/'")
    private String healthCheckEndpoint;

    @Min(value = 1, message = "Weight must be at least 1")
    @Max(value = 100, message = "Weight must not exceed 100")
    private Integer weight;
}
