package com.smartlb.customerservice.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutingRuleRequest {

    @NotBlank(message = "Path pattern cannot be empty")
    @Size(max = 255, message = "Path pattern cannot exceed 255 characters")
    private String pathPattern;

    private UUID targetServer;

    @Min(value = 0, message = "Priority must be non-negative")
    @Builder.Default
    private Integer priority = 1;

    @Pattern(regexp = "PATH_BASED|HEADER_BASED|AI_ROUTING", message = "Rule type must be PATH_BASED, HEADER_BASED, or AI_ROUTING")
    @Builder.Default
    private String ruleType = "PATH_BASED";

    @Builder.Default
    private Boolean enabled = true;
}
