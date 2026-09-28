package com.smartlb.customerservice.controller;

import com.smartlb.customerservice.dto.request.LoadBalancerConfigRequest;
import com.smartlb.customerservice.dto.response.ApiResponse;
import com.smartlb.customerservice.dto.response.LoadBalancerConfigResponse;
import com.smartlb.customerservice.security.UserPrincipal;
import com.smartlb.customerservice.service.LoadBalancerConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customer/websites/{websiteId}/lb-config")
@RequiredArgsConstructor
public class LoadBalancerConfigController {

    private final LoadBalancerConfigService lbConfigService;

    @GetMapping
    public ResponseEntity<ApiResponse<LoadBalancerConfigResponse>> getConfig(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID websiteId) {
        LoadBalancerConfigResponse response = lbConfigService.getConfig(principal.getOrganizationId(), websiteId);
        return ResponseEntity.ok(ApiResponse.success(response, "Load balancer configuration retrieved successfully"));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<LoadBalancerConfigResponse>> updateConfig(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID websiteId,
            @Valid @RequestBody LoadBalancerConfigRequest request) {
        LoadBalancerConfigResponse response = lbConfigService.updateConfig(
                principal.getOrganizationId(), websiteId, principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Load balancer configuration updated successfully"));
    }
}
