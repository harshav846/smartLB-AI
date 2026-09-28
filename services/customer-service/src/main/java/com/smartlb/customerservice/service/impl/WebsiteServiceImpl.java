package com.smartlb.customerservice.service.impl;

import com.smartlb.customerservice.dto.request.WebsiteRequest;
import com.smartlb.customerservice.dto.response.WebsiteResponse;
import com.smartlb.customerservice.entity.LoadBalancerConfig;
import com.smartlb.customerservice.entity.Website;
import com.smartlb.customerservice.exception.DuplicateResourceException;
import com.smartlb.customerservice.exception.ResourceNotFoundException;
import com.smartlb.customerservice.repository.LoadBalancerConfigRepository;
import com.smartlb.customerservice.repository.WebsiteRepository;
import com.smartlb.customerservice.service.WebsiteService;
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
public class WebsiteServiceImpl implements WebsiteService {

    private final WebsiteRepository websiteRepository;
    private final LoadBalancerConfigRepository lbConfigRepository;

    @Override
    @Transactional
    public WebsiteResponse createWebsite(UUID organizationId, UUID userId, WebsiteRequest request) {
        log.info("Creating website with domain '{}' for org ID: {}", request.getDomainName(), organizationId);

        if (websiteRepository.existsByDomainNameAndDeletedAtIsNull(request.getDomainName())) {
            throw new DuplicateResourceException("Domain name '" + request.getDomainName() + "' is already registered");
        }

        Website website = Website.builder()
                .organizationId(organizationId)
                .domainName(request.getDomainName().toLowerCase().trim())
                .displayName(request.getDisplayName().trim())
                .description(request.getDescription())
                .environment(request.getEnvironment() != null ? request.getEnvironment() : "PRODUCTION")
                .status("ACTIVE")
                .verificationStatus("VERIFIED")
                .createdBy(userId)
                .updatedBy(userId)
                .build();

        Website saved = websiteRepository.save(website);

        // Auto-provision default load balancer configuration
        LoadBalancerConfig defaultConfig = LoadBalancerConfig.builder()
                .websiteId(saved.getId())
                .algorithm("ROUND_ROBIN")
                .stickySessions(false)
                .sessionTimeout(3600)
                .healthCheckInterval(30)
                .requestTimeout(15)
                .retryAttempts(3)
                .createdBy(userId)
                .updatedBy(userId)
                .build();
        lbConfigRepository.save(defaultConfig);

        log.info("Website created with ID: {} and default LB configuration provisioned", saved.getId());
        return mapToResponse(saved);
    }

    @Override
    public List<WebsiteResponse> listWebsites(UUID organizationId) {
        return websiteRepository.findAllByOrganizationIdAndDeletedAtIsNull(organizationId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public WebsiteResponse getWebsite(UUID organizationId, UUID websiteId) {
        Website website = findWebsiteOrThrow(organizationId, websiteId);
        return mapToResponse(website);
    }

    @Override
    @Transactional
    public WebsiteResponse updateWebsite(UUID organizationId, UUID websiteId, WebsiteRequest request) {
        log.info("Updating website ID: {} for org ID: {}", websiteId, organizationId);
        Website website = findWebsiteOrThrow(organizationId, websiteId);

        String newDomain = request.getDomainName().toLowerCase().trim();
        if (!website.getDomainName().equalsIgnoreCase(newDomain) &&
                websiteRepository.existsByDomainNameAndDeletedAtIsNull(newDomain)) {
            throw new DuplicateResourceException("Domain name '" + newDomain + "' is already registered");
        }

        website.setDomainName(newDomain);
        website.setDisplayName(request.getDisplayName().trim());
        website.setDescription(request.getDescription());
        if (request.getEnvironment() != null) {
            website.setEnvironment(request.getEnvironment());
        }

        Website updated = websiteRepository.save(website);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deleteWebsite(UUID organizationId, UUID websiteId) {
        log.info("Soft-deleting website ID: {} for org ID: {}", websiteId, organizationId);
        Website website = findWebsiteOrThrow(organizationId, websiteId);
        website.setDeletedAt(Instant.now());
        website.setStatus("INACTIVE");
        websiteRepository.save(website);
    }

    private Website findWebsiteOrThrow(UUID organizationId, UUID websiteId) {
        return websiteRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(websiteId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Website with ID " + websiteId + " not found for this organization"));
    }

    private WebsiteResponse mapToResponse(Website w) {
        return WebsiteResponse.builder()
                .id(w.getId())
                .organizationId(w.getOrganizationId())
                .domainName(w.getDomainName())
                .displayName(w.getDisplayName())
                .description(w.getDescription())
                .environment(w.getEnvironment())
                .status(w.getStatus())
                .verificationStatus(w.getVerificationStatus())
                .createdAt(w.getCreatedAt())
                .updatedAt(w.getUpdatedAt())
                .build();
    }
}
