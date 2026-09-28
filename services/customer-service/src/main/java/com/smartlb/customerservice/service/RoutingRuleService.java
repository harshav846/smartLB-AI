package com.smartlb.customerservice.service;

import com.smartlb.customerservice.dto.request.RoutingRuleRequest;
import com.smartlb.customerservice.dto.response.RoutingRuleResponse;

import java.util.List;
import java.util.UUID;

public interface RoutingRuleService {

    RoutingRuleResponse createRule(UUID organizationId, UUID websiteId, UUID userId, RoutingRuleRequest request);

    List<RoutingRuleResponse> listRules(UUID organizationId, UUID websiteId);

    void deleteRule(UUID organizationId, UUID websiteId, UUID ruleId);
}
