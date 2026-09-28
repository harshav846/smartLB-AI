package com.smartlb.customerservice.service.impl;

import com.smartlb.customerservice.dto.request.RoutingRuleRequest;
import com.smartlb.customerservice.dto.response.RoutingRuleResponse;
import com.smartlb.customerservice.entity.RoutingRule;
import com.smartlb.customerservice.exception.ResourceNotFoundException;
import com.smartlb.customerservice.repository.RoutingRuleRepository;
import com.smartlb.customerservice.repository.WebsiteRepository;
import com.smartlb.customerservice.service.RoutingRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoutingRuleServiceImpl implements RoutingRuleService {

    private final RoutingRuleRepository ruleRepository;
    private final WebsiteRepository websiteRepository;

    @Override
    @Transactional
    public RoutingRuleResponse createRule(UUID organizationId, UUID websiteId, UUID userId, RoutingRuleRequest request) {
        log.info("Creating routing rule for path '{}' on website ID: {}", request.getPathPattern(), websiteId);
        verifyWebsiteOwnership(organizationId, websiteId);

        RoutingRule rule = RoutingRule.builder()
                .websiteId(websiteId)
                .pathPattern(request.getPathPattern().trim())
                .targetServer(request.getTargetServer())
                .priority(request.getPriority() != null ? request.getPriority() : 1)
                .ruleType(request.getRuleType() != null ? request.getRuleType() : "PATH_BASED")
                .enabled(request.getEnabled() != null ? request.getEnabled() : true)
                .createdBy(userId)
                .updatedBy(userId)
                .build();

        RoutingRule saved = ruleRepository.save(rule);
        return mapToResponse(saved);
    }

    @Override
    public List<RoutingRuleResponse> listRules(UUID organizationId, UUID websiteId) {
        verifyWebsiteOwnership(organizationId, websiteId);
        return ruleRepository.findAllByWebsiteIdAndDeletedAtIsNullOrderByPriorityAsc(websiteId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteRule(UUID organizationId, UUID websiteId, UUID ruleId) {
        verifyWebsiteOwnership(organizationId, websiteId);
        RoutingRule rule = ruleRepository.findByIdAndWebsiteIdAndDeletedAtIsNull(ruleId, websiteId)
                .orElseThrow(() -> new ResourceNotFoundException("Routing rule with ID " + ruleId + " not found"));
        rule.setDeletedAt(Instant.now());
        ruleRepository.save(rule);
    }

    private void verifyWebsiteOwnership(UUID organizationId, UUID websiteId) {
        websiteRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(websiteId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Website with ID " + websiteId + " not found for this organization"));
    }

    private RoutingRuleResponse mapToResponse(RoutingRule r) {
        return RoutingRuleResponse.builder()
                .id(r.getId())
                .websiteId(r.getWebsiteId())
                .pathPattern(r.getPathPattern())
                .targetServer(r.getTargetServer())
                .priority(r.getPriority())
                .ruleType(r.getRuleType())
                .enabled(r.getEnabled())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
