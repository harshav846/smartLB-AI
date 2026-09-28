package com.smartlb.customerservice.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class BackendServerRequest {

    @NotBlank(message = "Server name cannot be empty")
    @Size(max = 255, message = "Server name cannot exceed 255 characters")
    private String serverName;

    @NotBlank(message = "Private IP/host cannot be empty")
    @Size(max = 45, message = "Private IP cannot exceed 45 characters")
    private String privateIp;

    private String publicIp;

    @NotNull(message = "Port is required")
    @Min(value = 1, message = "Port must be at least 1")
    @Max(value = 65535, message = "Port cannot exceed 65535")
    private Integer port;

    @Pattern(regexp = "HTTP|HTTPS", message = "Protocol must be HTTP or HTTPS")
    @Builder.Default
    private String protocol = "HTTP";

    @Min(value = 1, message = "Weight must be at least 1")
    @Builder.Default
    private Integer weight = 1;

    @Min(value = 0, message = "Priority must be non-negative")
    @Builder.Default
    private Integer priority = 1;

    @Min(value = 1, message = "Max connections must be at least 1")
    @Builder.Default
    private Integer maxConnections = 1000;
}
