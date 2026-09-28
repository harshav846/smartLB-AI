package com.smartlb.customerservice.service;

import com.smartlb.customerservice.dto.request.WebsiteRequest;
import com.smartlb.customerservice.dto.response.WebsiteResponse;
import com.smartlb.customerservice.entity.LoadBalancerConfig;
import com.smartlb.customerservice.entity.Website;
import com.smartlb.customerservice.exception.DuplicateResourceException;
import com.smartlb.customerservice.exception.ResourceNotFoundException;
import com.smartlb.customerservice.repository.LoadBalancerConfigRepository;
import com.smartlb.customerservice.repository.WebsiteRepository;
import com.smartlb.customerservice.service.impl.WebsiteServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebsiteServiceTest {

    @Mock
    private WebsiteRepository websiteRepository;

    @Mock
    private LoadBalancerConfigRepository lbConfigRepository;

    @InjectMocks
    private WebsiteServiceImpl websiteService;

    private UUID organizationId;
    private UUID userId;
    private UUID websiteId;
    private Website testWebsite;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        userId = UUID.randomUUID();
        websiteId = UUID.randomUUID();

        testWebsite = Website.builder()
                .id(websiteId)
                .organizationId(organizationId)
                .domainName("api.example.com")
                .displayName("Example API")
                .description("Production API")
                .environment("PRODUCTION")
                .status("ACTIVE")
                .verificationStatus("VERIFIED")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Should successfully create a website and provision default load balancer config")
    void createWebsite_success() {
        WebsiteRequest request = WebsiteRequest.builder()
                .domainName("api.example.com")
                .displayName("Example API")
                .description("Production API")
                .environment("PRODUCTION")
                .build();

        when(websiteRepository.existsByDomainNameAndDeletedAtIsNull("api.example.com")).thenReturn(false);
        when(websiteRepository.save(any(Website.class))).thenAnswer(inv -> {
            Website w = inv.getArgument(0);
            w.setId(websiteId);
            return w;
        });
        when(lbConfigRepository.save(any(LoadBalancerConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        WebsiteResponse response = websiteService.createWebsite(organizationId, userId, request);

        assertThat(response).isNotNull();
        assertThat(response.getDomainName()).isEqualTo("api.example.com");
        assertThat(response.getOrganizationId()).isEqualTo(organizationId);
        verify(websiteRepository).save(any(Website.class));
        verify(lbConfigRepository).save(any(LoadBalancerConfig.class));
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when domain is already registered")
    void createWebsite_duplicateDomain_throwsDuplicate() {
        WebsiteRequest request = WebsiteRequest.builder()
                .domainName("api.example.com")
                .displayName("Example API")
                .build();

        when(websiteRepository.existsByDomainNameAndDeletedAtIsNull("api.example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () ->
                websiteService.createWebsite(organizationId, userId, request));
        verify(websiteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should list only tenant-scoped websites")
    void listWebsites_returnsTenantWebsitesOnly() {
        when(websiteRepository.findAllByOrganizationIdAndDeletedAtIsNull(organizationId))
                .thenReturn(List.of(testWebsite));

        List<WebsiteResponse> responses = websiteService.listWebsites(organizationId);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getId()).isEqualTo(websiteId);
    }

    @Test
    @DisplayName("Should reject access to website belonging to different organization")
    void getWebsite_differentOrg_throwsResourceNotFound() {
        UUID otherOrgId = UUID.randomUUID();
        when(websiteRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(websiteId, otherOrgId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                websiteService.getWebsite(otherOrgId, websiteId));
    }

    @Test
    @DisplayName("Should soft-delete website successfully")
    void deleteWebsite_softDeletesSuccessfully() {
        when(websiteRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(websiteId, organizationId))
                .thenReturn(Optional.of(testWebsite));

        websiteService.deleteWebsite(organizationId, websiteId);

        assertThat(testWebsite.getDeletedAt()).isNotNull();
        assertThat(testWebsite.getStatus()).isEqualTo("INACTIVE");
        verify(websiteRepository).save(testWebsite);
    }
}
