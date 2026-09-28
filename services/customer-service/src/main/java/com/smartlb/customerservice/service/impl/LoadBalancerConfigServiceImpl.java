package com.smartlb.customerservice.service.impl;

import com.smartlb.customerservice.dto.request.LoadBalancerConfigRequest;
import com.smartlb.customerservice.dto.response.LoadBalancerConfigResponse;
import com.smartlb.customerservice.entity.LoadBalancerConfig;
import com.smartlb.customerservice.exception.ResourceNotFoundException;
import com.smartlb.customerservice.repository.LoadBalancerConfigRepository;
import com.smartlb.customerservice.repository.WebsiteRepository;
import com.smartlb.customerservice.service.LoadBalancerConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoadBalancerConfigServiceImpl implements LoadBalancerConfigService {

    private final LoadBalancerConfigRepository lbConfigRepository;
    private final WebsiteRepository websiteRepository;

    @Override
    public LoadBalancerConfigResponse getConfig(UUID organizationId, UUID websiteId) {
        verifyWebsiteOwnership(organizationId, websiteId);
        LoadBalancerConfig config = lbConfigRepository.findByWebsiteIdAndDeletedAtIsNull(websiteId)
                .orElseThrow(() -> new ResourceNotFoundException("Load balancer configuration not found for website ID " + websiteId));
        return mapToResponse(config);
    }

    @Override
    @Transactional
    public LoadBalancerConfigResponse updateConfig(UUID organizationId, UUID websiteId, UUID userId, LoadBalancerConfigRequest request) {
        log.info("Updating LB config for website ID: {} in org ID: {}", websiteId, organizationId);
        verifyWebsiteOwnership(organizationId, websiteId);

        LoadBalancerConfig config = lbConfigRepository.findByWebsiteIdAndDeletedAtIsNull(websiteId)
                .orElseGet(() -> LoadBalancerConfig.builder()
                        .websiteId(websiteId)
                        .createdBy(userId)
                        .build());

        config.setAlgorithm(request.getAlgorithm());
        if (request.getStickySessions() != null) config.setStickySessions(request.getStickySessions());
        if (request.getSessionTimeout() != null) config.setSessionTimeout(request.getSessionTimeout());
        if (request.getHealthCheckInterval() != null) config.setHealthCheckInterval(request.getHealthCheckInterval());
        if (request.getRequestTimeout() != null) config.setRequestTimeout(request.getRequestTimeout());
        if (request.getRetryAttempts() != null) config.setRetryAttempts(request.getRetryAttempts());
        config.setUpdatedBy(userId);

        LoadBalancerConfig saved = lbConfigRepository.save(config);
        return mapToResponse(saved);
    }

    private void verifyWebsiteOwnership(UUID organizationId, UUID websiteId) {
        websiteRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(websiteId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Website with ID " + websiteId + " not found for this organization"));
    }

    private LoadBalancerConfigResponse mapToResponse(LoadBalancerConfig c) {
        return LoadBalancerConfigResponse.builder()
                .id(c.getId())
                .websiteId(c.getWebsiteId())
                .algorithm(c.getAlgorithm())
                .stickySessions(c.getStickySessions())
                .sessionTimeout(c.getSessionTimeout())
                .healthCheckInterval(c.getHealthCheckInterval())
                .requestTimeout(c.getRequestTimeout())
                .retryAttempts(c.getRetryAttempts())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
