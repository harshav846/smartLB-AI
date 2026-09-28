package com.smartlb.customerservice.controller;

import com.smartlb.customerservice.dto.request.WebsiteRequest;
import com.smartlb.customerservice.dto.response.ApiResponse;
import com.smartlb.customerservice.dto.response.WebsiteResponse;
import com.smartlb.customerservice.security.UserPrincipal;
import com.smartlb.customerservice.service.WebsiteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customer/websites")
@RequiredArgsConstructor
public class WebsiteController {

    private final WebsiteService websiteService;

    @PostMapping
    public ResponseEntity<ApiResponse<WebsiteResponse>> createWebsite(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody WebsiteRequest request) {
        WebsiteResponse response = websiteService.createWebsite(principal.getOrganizationId(), principal.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Website created successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<WebsiteResponse>>> listWebsites(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<WebsiteResponse> response = websiteService.listWebsites(principal.getOrganizationId());
        return ResponseEntity.ok(ApiResponse.success(response, "Websites retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WebsiteResponse>> getWebsite(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        WebsiteResponse response = websiteService.getWebsite(principal.getOrganizationId(), id);
        return ResponseEntity.ok(ApiResponse.success(response, "Website retrieved successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<WebsiteResponse>> updateWebsite(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody WebsiteRequest request) {
        WebsiteResponse response = websiteService.updateWebsite(principal.getOrganizationId(), id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Website updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteWebsite(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        websiteService.deleteWebsite(principal.getOrganizationId(), id);
        return ResponseEntity.ok(ApiResponse.success(null, "Website deleted successfully"));
    }
}
