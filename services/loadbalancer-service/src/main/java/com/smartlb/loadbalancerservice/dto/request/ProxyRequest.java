package com.smartlb.loadbalancerservice.dto.request;

import com.smartlb.loadbalancerservice.algorithm.LoadBalancingAlgorithm;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * Request DTO for routing traffic through the SmartLB-AI proxy engine.
 */
@Getter
@Setter
@NoArgsConstructor
public class ProxyRequest {

    /**
     * HTTP method to forward (GET, POST, PUT, DELETE, PATCH, etc.).
     */
    @NotBlank(message = "HTTP method must not be blank")
    private String method;

    /**
     * Path + query string to append to the backend instance base URL.
     * Example: "/api/v1/products?page=1"
     */
    @NotBlank(message = "Path must not be blank")
    private String path;

    /**
     * Optional request body (JSON string). Used for POST/PUT/PATCH.
     */
    private String body;

    /**
     * HTTP headers to forward to the backend (optional).
     */
    private Map<String, String> headers;

    /**
     * Load balancing algorithm override. If null, defaults to ROUND_ROBIN.
     */
    private LoadBalancingAlgorithm algorithm;

    /**
     * Client IP, used by IP_HASH algorithm.
     * If null, the filter will populate this from the remote address.
     */
    private String clientIp;
}
