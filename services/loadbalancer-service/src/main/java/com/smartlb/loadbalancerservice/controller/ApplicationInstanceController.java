package com.smartlb.loadbalancerservice.controller;

import com.smartlb.loadbalancerservice.dto.request.RegisterInstanceRequest;
import com.smartlb.loadbalancerservice.dto.request.UpdateInstanceRequest;
import com.smartlb.loadbalancerservice.dto.request.UpdateStatusRequest;
import com.smartlb.loadbalancerservice.dto.response.ApiResponse;
import com.smartlb.loadbalancerservice.dto.response.InstanceHealthResponse;
import com.smartlb.loadbalancerservice.dto.response.InstanceResponse;
import com.smartlb.loadbalancerservice.security.UserPrincipal;
import com.smartlb.loadbalancerservice.service.ApplicationInstanceService;
import com.smartlb.loadbalancerservice.service.HealthMonitoringService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller exposing endpoints for server/application instance management and health tracking.
 * Strictly enforces multi-tenant isolation via the authenticated {@link UserPrincipal}.
 */
@Slf4j
@RestController
@RequestMapping("/api/instances")
@RequiredArgsConstructor
public class ApplicationInstanceController {

    private final ApplicationInstanceService instanceService;
    private final HealthMonitoringService healthMonitoringService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'OPERATOR')")
    public ResponseEntity<ApiResponse<InstanceResponse>> registerInstance(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody RegisterInstanceRequest request) {
        log.info("Received request to register instance '{}' for tenant ID: {}", request.getName(), principal.getOrganizationId());
        InstanceResponse response = instanceService.registerInstance(principal.getOrganizationId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Application instance registered successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<InstanceResponse>>> listInstances(
            @AuthenticationPrincipal UserPrincipal principal) {
        log.debug("Listing application instances for tenant ID: {}", principal.getOrganizationId());
        List<InstanceResponse> instances = instanceService.listTenantInstances(principal.getOrganizationId());
        return ResponseEntity.ok(ApiResponse.success(instances, "Tenant instances retrieved successfully"));
    }

    @GetMapping("/healthy")
    public ResponseEntity<ApiResponse<List<InstanceResponse>>> getHealthyInstances(
            @AuthenticationPrincipal UserPrincipal principal) {
        log.debug("Retrieving healthy application instances for tenant ID: {}", principal.getOrganizationId());
        List<InstanceResponse> healthyInstances = instanceService.getHealthyInstances(principal.getOrganizationId());
        return ResponseEntity.ok(ApiResponse.success(healthyInstances, "Healthy instances retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InstanceResponse>> getInstance(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") UUID instanceId) {
        log.debug("Retrieving instance ID: {} for tenant ID: {}", instanceId, principal.getOrganizationId());
        InstanceResponse response = instanceService.getInstance(principal.getOrganizationId(), instanceId);
        return ResponseEntity.ok(ApiResponse.success(response, "Instance retrieved successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'OPERATOR')")
    public ResponseEntity<ApiResponse<InstanceResponse>> updateInstance(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") UUID instanceId,
            @Valid @RequestBody UpdateInstanceRequest request) {
        log.info("Updating instance ID: {} for tenant ID: {}", instanceId, principal.getOrganizationId());
        InstanceResponse response = instanceService.updateInstance(principal.getOrganizationId(), instanceId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Instance updated successfully"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'OPERATOR')")
    public ResponseEntity<ApiResponse<InstanceResponse>> updateInstanceStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") UUID instanceId,
            @Valid @RequestBody UpdateStatusRequest request) {
        log.info("Updating status of instance ID: {} to {} for tenant ID: {}", instanceId, request.getStatus(), principal.getOrganizationId());
        InstanceResponse response = instanceService.updateInstanceStatus(principal.getOrganizationId(), instanceId, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success(response, "Instance status updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'OPERATOR')")
    public ResponseEntity<ApiResponse<Void>> deleteInstance(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") UUID instanceId) {
        log.info("Deleting instance ID: {} for tenant ID: {}", instanceId, principal.getOrganizationId());
        instanceService.deleteInstance(principal.getOrganizationId(), instanceId);
        return ResponseEntity.ok(ApiResponse.success(null, "Instance decommissioned successfully"));
    }

    @GetMapping("/{id}/health")
    public ResponseEntity<ApiResponse<InstanceHealthResponse>> getInstanceHealth(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") UUID instanceId) {
        log.debug("Retrieving stored health status for instance ID: {} for tenant ID: {}", instanceId, principal.getOrganizationId());
        InstanceHealthResponse healthResponse = healthMonitoringService.getSingleInstanceHealth(principal.getOrganizationId(), instanceId);
        return ResponseEntity.ok(ApiResponse.success(healthResponse, "Instance health status retrieved successfully"));
    }
}
