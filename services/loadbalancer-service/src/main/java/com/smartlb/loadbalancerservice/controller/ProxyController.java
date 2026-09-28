package com.smartlb.loadbalancerservice.controller;

import com.smartlb.loadbalancerservice.algorithm.LoadBalancingAlgorithm;
import com.smartlb.loadbalancerservice.dto.request.ProxyRequest;
import com.smartlb.loadbalancerservice.dto.response.ApiResponse;
import com.smartlb.loadbalancerservice.dto.response.ProxyResponse;
import com.smartlb.loadbalancerservice.entity.ApplicationInstance;
import com.smartlb.loadbalancerservice.security.UserPrincipal;
import com.smartlb.loadbalancerservice.service.TrafficProxyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller exposing the SmartLB-AI traffic proxy endpoints.
 * <p>
 * POST /api/v1/proxy          — forward a request to a selected backend instance
 * GET  /api/v1/proxy/select   — dry-run: show which instance would be chosen
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/proxy")
@RequiredArgsConstructor
public class ProxyController {

    private final TrafficProxyService trafficProxyService;

    /**
     * Forward a request through the load balancer to a healthy backend instance.
     *
     * @param principal authenticated user (contains organizationId + roles)
     * @param request   proxy request payload
     * @param servletRequest underlying HTTP request (for extracting client IP)
     * @return upstream response wrapped in {@link ProxyResponse}
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<ProxyResponse> proxy(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ProxyRequest request,
            HttpServletRequest servletRequest) {

        UUID organizationId = principal.getOrganizationId();

        // Populate clientIp from request if not supplied in body
        String clientIp = request.getClientIp() != null
                ? request.getClientIp()
                : resolveClientIp(servletRequest);

        // Merge servlet request headers with any supplied in the body
        Map<String, String> mergedHeaders = collectHeaders(servletRequest);
        if (request.getHeaders() != null) {
            mergedHeaders.putAll(request.getHeaders());
        }

        log.debug("[PROXY] org={} method={} path={} algo={} ip={}",
                organizationId, request.getMethod(), request.getPath(),
                request.getAlgorithm(), clientIp);

        ProxyResponse proxyResponse = trafficProxyService.proxy(
                organizationId,
                request.getMethod(),
                request.getPath(),
                request.getBody(),
                mergedHeaders,
                clientIp,
                request.getAlgorithm()
        );

        // Mirror the upstream HTTP status code back to the caller
        HttpStatus status = HttpStatus.resolve(proxyResponse.getHttpStatus());
        if (status == null) status = HttpStatus.OK;
        return ResponseEntity.status(status).body(proxyResponse);
    }

    /**
     * Dry-run endpoint — returns which instance would be selected without sending any request.
     *
     * @param principal  authenticated user
     * @param algorithm  optional algorithm override (defaults to ROUND_ROBIN)
     * @param clientIp   optional client IP override (for IP_HASH)
     * @param servletRequest underlying servlet request
     * @return ApiResponse wrapping the selected instance details
     */
    @GetMapping("/select")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> selectInstance(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String algorithm,
            @RequestParam(required = false) String clientIp,
            HttpServletRequest servletRequest) {

        UUID organizationId = principal.getOrganizationId();

        String resolvedIp = clientIp != null ? clientIp : resolveClientIp(servletRequest);
        LoadBalancingAlgorithm algo = LoadBalancingAlgorithm.fromString(algorithm);

        ApplicationInstance selected = trafficProxyService.selectInstance(organizationId, resolvedIp, algo);

        Map<String, Object> result = new HashMap<>();
        result.put("instanceId", selected.getId());
        result.put("instanceName", selected.getName());
        result.put("host", selected.getHost());
        result.put("port", selected.getPort());
        result.put("baseUrl", selected.getBaseUrl());
        result.put("algorithm", algo.name());
        result.put("clientIp", resolvedIp);

        return ResponseEntity.ok(ApiResponse.success(result, "Instance selected"));
    }

    /* ── helpers ───────────────────────────────────────────────────────── */

    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private Map<String, String> collectHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        while (names != null && names.hasMoreElements()) {
            String name = names.nextElement();
            headers.put(name, request.getHeader(name));
        }
        return headers;
    }
}
