package com.smartlb.customerservice.controller;

import com.smartlb.customerservice.dto.request.BackendServerRequest;
import com.smartlb.customerservice.dto.response.ApiResponse;
import com.smartlb.customerservice.dto.response.BackendServerResponse;
import com.smartlb.customerservice.security.UserPrincipal;
import com.smartlb.customerservice.service.BackendServerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customer/websites/{websiteId}/servers")
@RequiredArgsConstructor
public class BackendServerController {

    private final BackendServerService backendServerService;

    @PostMapping
    public ResponseEntity<ApiResponse<BackendServerResponse>> registerServer(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID websiteId,
            @Valid @RequestBody BackendServerRequest request) {
        BackendServerResponse response = backendServerService.registerBackendServer(
                principal.getOrganizationId(), websiteId, principal.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Backend server registered successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BackendServerResponse>>> listServers(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID websiteId) {
        List<BackendServerResponse> response = backendServerService.listBackendServers(
                principal.getOrganizationId(), websiteId);
        return ResponseEntity.ok(ApiResponse.success(response, "Backend servers retrieved successfully"));
    }

    @GetMapping("/{serverId}")
    public ResponseEntity<ApiResponse<BackendServerResponse>> getServer(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID websiteId,
            @PathVariable UUID serverId) {
        BackendServerResponse response = backendServerService.getBackendServer(
                principal.getOrganizationId(), websiteId, serverId);
        return ResponseEntity.ok(ApiResponse.success(response, "Backend server retrieved successfully"));
    }

    @PutMapping("/{serverId}")
    public ResponseEntity<ApiResponse<BackendServerResponse>> updateServer(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID websiteId,
            @PathVariable UUID serverId,
            @Valid @RequestBody BackendServerRequest request) {
        BackendServerResponse response = backendServerService.updateBackendServer(
                principal.getOrganizationId(), websiteId, serverId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Backend server updated successfully"));
    }

    @DeleteMapping("/{serverId}")
    public ResponseEntity<ApiResponse<Void>> deleteServer(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID websiteId,
            @PathVariable UUID serverId) {
        backendServerService.deleteBackendServer(principal.getOrganizationId(), websiteId, serverId);
        return ResponseEntity.ok(ApiResponse.success(null, "Backend server deleted successfully"));
    }

    @PatchMapping("/{serverId}/status")
    public ResponseEntity<ApiResponse<BackendServerResponse>> updateServerStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID websiteId,
            @PathVariable UUID serverId,
            @RequestBody Map<String, String> body) {
        String status = body.get("status");
        BackendServerResponse response = backendServerService.updateServerStatus(
                principal.getOrganizationId(), websiteId, serverId, status);
        return ResponseEntity.ok(ApiResponse.success(response, "Backend server status updated to " + status));
    }
}
