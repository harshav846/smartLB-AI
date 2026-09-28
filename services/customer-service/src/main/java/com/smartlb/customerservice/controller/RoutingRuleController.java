package com.smartlb.customerservice.controller;

import com.smartlb.customerservice.dto.request.RoutingRuleRequest;
import com.smartlb.customerservice.dto.response.ApiResponse;
import com.smartlb.customerservice.dto.response.RoutingRuleResponse;
import com.smartlb.customerservice.security.UserPrincipal;
import com.smartlb.customerservice.service.RoutingRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customer/websites/{websiteId}/rules")
@RequiredArgsConstructor
public class RoutingRuleController {

    private final RoutingRuleService routingRuleService;

    @PostMapping
    public ResponseEntity<ApiResponse<RoutingRuleResponse>> createRule(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID websiteId,
            @Valid @RequestBody RoutingRuleRequest request) {
        RoutingRuleResponse response = routingRuleService.createRule(
                principal.getOrganizationId(), websiteId, principal.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Routing rule created successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoutingRuleResponse>>> listRules(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID websiteId) {
        List<RoutingRuleResponse> response = routingRuleService.listRules(
                principal.getOrganizationId(), websiteId);
        return ResponseEntity.ok(ApiResponse.success(response, "Routing rules retrieved successfully"));
    }

    @DeleteMapping("/{ruleId}")
    public ResponseEntity<ApiResponse<Void>> deleteRule(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID websiteId,
            @PathVariable UUID ruleId) {
        routingRuleService.deleteRule(principal.getOrganizationId(), websiteId, ruleId);
        return ResponseEntity.ok(ApiResponse.success(null, "Routing rule deleted successfully"));
    }
}
