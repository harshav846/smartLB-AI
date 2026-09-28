package com.smartlb.customerservice.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoadBalancerConfigRequest {

    @NotBlank(message = "Algorithm cannot be blank")
    @Pattern(
        regexp = "ROUND_ROBIN|LEAST_CONNECTIONS|WEIGHTED_ROUND_ROBIN|IP_HASH|AI_ROUTING",
        message = "Algorithm must be ROUND_ROBIN, LEAST_CONNECTIONS, WEIGHTED_ROUND_ROBIN, IP_HASH, or AI_ROUTING"
    )
    private String algorithm;

    @Builder.Default
    private Boolean stickySessions = false;

    @Min(value = 0, message = "Session timeout must be non-negative")
    @Builder.Default
    private Integer sessionTimeout = 3600;

    @Min(value = 1, message = "Health check interval must be at least 1 second")
    @Builder.Default
    private Integer healthCheckInterval = 30;

    @Min(value = 1, message = "Request timeout must be at least 1 second")
    @Builder.Default
    private Integer requestTimeout = 15;

    @Min(value = 0, message = "Retry attempts must be non-negative")
    @Builder.Default
    private Integer retryAttempts = 3;
}
