package com.smartlb.customerservice.service;

import com.smartlb.customerservice.dto.request.BackendServerRequest;
import com.smartlb.customerservice.dto.response.BackendServerResponse;
import com.smartlb.customerservice.entity.BackendServer;
import com.smartlb.customerservice.entity.Website;
import com.smartlb.customerservice.exception.DuplicateResourceException;
import com.smartlb.customerservice.exception.ResourceNotFoundException;
import com.smartlb.customerservice.exception.SsrfException;
import com.smartlb.customerservice.repository.BackendServerRepository;
import com.smartlb.customerservice.repository.WebsiteRepository;
import com.smartlb.customerservice.security.ssrf.SsrfValidator;
import com.smartlb.customerservice.service.impl.BackendServerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BackendServerServiceTest {

    @Mock
    private BackendServerRepository backendRepository;

    @Mock
    private WebsiteRepository websiteRepository;

    @Mock
    private SsrfValidator ssrfValidator;

    @InjectMocks
    private BackendServerServiceImpl backendService;

    private UUID organizationId;
    private UUID userId;
    private UUID websiteId;
    private UUID serverId;
    private Website testWebsite;
    private BackendServer testServer;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        userId = UUID.randomUUID();
        websiteId = UUID.randomUUID();
        serverId = UUID.randomUUID();

        testWebsite = Website.builder()
                .id(websiteId)
                .organizationId(organizationId)
                .domainName("api.example.com")
                .displayName("Example API")
                .build();

        testServer = BackendServer.builder()
                .id(serverId)
                .websiteId(websiteId)
                .serverName("srv-1")
                .privateIp("192.168.1.100")
                .port(8080)
                .protocol("HTTP")
                .weight(1)
                .priority(1)
                .maxConnections(1000)
                .currentConnections(0)
                .cpuUsage(BigDecimal.ZERO)
                .memoryUsage(BigDecimal.ZERO)
                .diskUsage(BigDecimal.ZERO)
                .healthStatus("HEALTHY")
                .serverStatus("ONLINE")
                .responseTimeMs(15)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Should successfully register a backend server with SSRF validation")
    void registerBackendServer_success() {
        BackendServerRequest request = BackendServerRequest.builder()
                .serverName("srv-1")
                .privateIp("192.168.1.100")
                .port(8080)
                .protocol("HTTP")
                .weight(1)
                .build();

        when(websiteRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(websiteId, organizationId))
                .thenReturn(Optional.of(testWebsite));
        doNothing().when(ssrfValidator).validateTarget("192.168.1.100", 8080);
        when(backendRepository.existsByWebsiteIdAndServerNameAndDeletedAtIsNull(websiteId, "srv-1")).thenReturn(false);
        when(backendRepository.existsByWebsiteIdAndPrivateIpAndPortAndDeletedAtIsNull(websiteId, "192.168.1.100", 8080)).thenReturn(false);
        when(backendRepository.save(any(BackendServer.class))).thenAnswer(inv -> {
            BackendServer s = inv.getArgument(0);
            s.setId(serverId);
            return s;
        });

        BackendServerResponse response = backendService.registerBackendServer(organizationId, websiteId, userId, request);

        assertThat(response).isNotNull();
        assertThat(response.getServerName()).isEqualTo("srv-1");
        assertThat(response.getHealthStatus()).isEqualTo("HEALTHY");
        assertThat(response.getServerStatus()).isEqualTo("ONLINE");
        verify(ssrfValidator).validateTarget("192.168.1.100", 8080);
        verify(backendRepository).save(any(BackendServer.class));
    }

    @Test
    @DisplayName("Should reject registering backend if caller's org does not own website")
    void registerBackendServer_crossTenant_throwsResourceNotFound() {
        UUID strangerOrgId = UUID.randomUUID();
        BackendServerRequest request = BackendServerRequest.builder()
                .serverName("srv-1")
                .privateIp("192.168.1.100")
                .port(8080)
                .build();

        when(websiteRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(websiteId, strangerOrgId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                backendService.registerBackendServer(strangerOrgId, websiteId, userId, request));
        verify(backendRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject registering backend when SSRF validator fails")
    void registerBackendServer_ssrfBlocked_throwsSsrfException() {
        BackendServerRequest request = BackendServerRequest.builder()
                .serverName("srv-metadata")
                .privateIp("169.254.169.254")
                .port(80)
                .build();

        when(websiteRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(websiteId, organizationId))
                .thenReturn(Optional.of(testWebsite));
        doThrow(new SsrfException("Access to cloud metadata is forbidden"))
                .when(ssrfValidator).validateTarget("169.254.169.254", 80);

        assertThrows(SsrfException.class, () ->
                backendService.registerBackendServer(organizationId, websiteId, userId, request));
        verify(backendRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully update server status to DRAINING")
    void updateServerStatus_draining_success() {
        when(websiteRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(websiteId, organizationId))
                .thenReturn(Optional.of(testWebsite));
        when(backendRepository.findByIdAndWebsiteIdAndDeletedAtIsNull(serverId, websiteId))
                .thenReturn(Optional.of(testServer));
        when(backendRepository.save(any(BackendServer.class))).thenAnswer(inv -> inv.getArgument(0));

        BackendServerResponse response = backendService.updateServerStatus(organizationId, websiteId, serverId, "DRAINING");

        assertThat(response.getServerStatus()).isEqualTo("DRAINING");
    }

    @Test
    @DisplayName("Should reject invalid server status update")
    void updateServerStatus_invalid_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                backendService.updateServerStatus(organizationId, websiteId, serverId, "BOGUS_STATUS"));
    }
}
