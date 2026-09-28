package com.smartlb.customerservice.service.impl;

import com.smartlb.customerservice.dto.request.BackendServerRequest;
import com.smartlb.customerservice.dto.response.BackendServerResponse;
import com.smartlb.customerservice.entity.BackendServer;
import com.smartlb.customerservice.entity.Website;
import com.smartlb.customerservice.exception.DuplicateResourceException;
import com.smartlb.customerservice.exception.ResourceNotFoundException;
import com.smartlb.customerservice.repository.BackendServerRepository;
import com.smartlb.customerservice.repository.WebsiteRepository;
import com.smartlb.customerservice.security.ssrf.SsrfValidator;
import com.smartlb.customerservice.service.BackendServerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BackendServerServiceImpl implements BackendServerService {

    private static final Set<String> VALID_SERVER_STATUSES = Set.of("ONLINE", "OFFLINE", "MAINTENANCE", "DRAINING");

    private final BackendServerRepository backendRepository;
    private final WebsiteRepository websiteRepository;
    private final SsrfValidator ssrfValidator;

    @Override
    @Transactional
    public BackendServerResponse registerBackendServer(UUID organizationId, UUID websiteId, UUID userId, BackendServerRequest request) {
        log.info("Registering backend server '{}' for website ID: {} in org ID: {}", request.getServerName(), websiteId, organizationId);
        verifyWebsiteOwnership(organizationId, websiteId);

        ssrfValidator.validateTarget(request.getPrivateIp(), request.getPort());

        if (backendRepository.existsByWebsiteIdAndServerNameAndDeletedAtIsNull(websiteId, request.getServerName())) {
            throw new DuplicateResourceException("Server with name '" + request.getServerName() + "' already exists for this website");
        }

        if (backendRepository.existsByWebsiteIdAndPrivateIpAndPortAndDeletedAtIsNull(websiteId, request.getPrivateIp(), request.getPort())) {
            throw new DuplicateResourceException("Server with address " + request.getPrivateIp() + ":" + request.getPort() + " already exists for this website");
        }

        BackendServer server = BackendServer.builder()
                .websiteId(websiteId)
                .serverName(request.getServerName().trim())
                .privateIp(request.getPrivateIp().trim())
                .publicIp(request.getPublicIp())
                .port(request.getPort())
                .protocol(request.getProtocol() != null ? request.getProtocol().toUpperCase() : "HTTP")
                .weight(request.getWeight() != null ? request.getWeight() : 1)
                .priority(request.getPriority() != null ? request.getPriority() : 1)
                .maxConnections(request.getMaxConnections() != null ? request.getMaxConnections() : 1000)
                .currentConnections(0)
                .cpuUsage(BigDecimal.ZERO)
                .memoryUsage(BigDecimal.ZERO)
                .diskUsage(BigDecimal.ZERO)
                .healthStatus("HEALTHY")
                .serverStatus("ONLINE")
                .responseTimeMs(0)
                .lastHealthCheck(Instant.now())
                .createdBy(userId)
                .updatedBy(userId)
                .build();

        BackendServer saved = backendRepository.save(server);
        log.info("Backend server registered successfully with ID: {}", saved.getId());
        return mapToResponse(saved);
    }

    @Override
    public List<BackendServerResponse> listBackendServers(UUID organizationId, UUID websiteId) {
        verifyWebsiteOwnership(organizationId, websiteId);
        return backendRepository.findAllByWebsiteIdAndDeletedAtIsNull(websiteId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public BackendServerResponse getBackendServer(UUID organizationId, UUID websiteId, UUID serverId) {
        verifyWebsiteOwnership(organizationId, websiteId);
        BackendServer server = findServerOrThrow(websiteId, serverId);
        return mapToResponse(server);
    }

    @Override
    @Transactional
    public BackendServerResponse updateBackendServer(UUID organizationId, UUID websiteId, UUID serverId, BackendServerRequest request) {
        verifyWebsiteOwnership(organizationId, websiteId);
        BackendServer server = findServerOrThrow(websiteId, serverId);

        ssrfValidator.validateTarget(request.getPrivateIp(), request.getPort());

        if (!server.getServerName().equals(request.getServerName()) &&
                backendRepository.existsByWebsiteIdAndServerNameAndDeletedAtIsNull(websiteId, request.getServerName())) {
            throw new DuplicateResourceException("Server with name '" + request.getServerName() + "' already exists for this website");
        }

        if ((!server.getPrivateIp().equals(request.getPrivateIp()) || !server.getPort().equals(request.getPort())) &&
                backendRepository.existsByWebsiteIdAndPrivateIpAndPortAndDeletedAtIsNull(websiteId, request.getPrivateIp(), request.getPort())) {
            throw new DuplicateResourceException("Server with address " + request.getPrivateIp() + ":" + request.getPort() + " already exists for this website");
        }

        server.setServerName(request.getServerName().trim());
        server.setPrivateIp(request.getPrivateIp().trim());
        server.setPublicIp(request.getPublicIp());
        server.setPort(request.getPort());
        if (request.getProtocol() != null) server.setProtocol(request.getProtocol().toUpperCase());
        if (request.getWeight() != null) server.setWeight(request.getWeight());
        if (request.getPriority() != null) server.setPriority(request.getPriority());
        if (request.getMaxConnections() != null) server.setMaxConnections(request.getMaxConnections());

        BackendServer updated = backendRepository.save(server);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deleteBackendServer(UUID organizationId, UUID websiteId, UUID serverId) {
        log.info("Deleting backend server ID: {} for website ID: {}", serverId, websiteId);
        verifyWebsiteOwnership(organizationId, websiteId);
        BackendServer server = findServerOrThrow(websiteId, serverId);
        server.setDeletedAt(Instant.now());
        server.setServerStatus("OFFLINE");
        backendRepository.save(server);
    }

    @Override
    @Transactional
    public BackendServerResponse updateServerStatus(UUID organizationId, UUID websiteId, UUID serverId, String status) {
        String normalizedStatus = status != null ? status.toUpperCase().trim() : "";
        if (!VALID_SERVER_STATUSES.contains(normalizedStatus)) {
            throw new IllegalArgumentException("Invalid server status: " + status + ". Allowed: " + VALID_SERVER_STATUSES);
        }

        verifyWebsiteOwnership(organizationId, websiteId);
        BackendServer server = findServerOrThrow(websiteId, serverId);
        server.setServerStatus(normalizedStatus);
        BackendServer updated = backendRepository.save(server);
        log.info("Updated server ID: {} status to {}", serverId, normalizedStatus);
        return mapToResponse(updated);
    }

    private void verifyWebsiteOwnership(UUID organizationId, UUID websiteId) {
        websiteRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(websiteId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Website with ID " + websiteId + " not found for this organization"));
    }

    private BackendServer findServerOrThrow(UUID websiteId, UUID serverId) {
        return backendRepository.findByIdAndWebsiteIdAndDeletedAtIsNull(serverId, websiteId)
                .orElseThrow(() -> new ResourceNotFoundException("Backend server with ID " + serverId + " not found for website ID " + websiteId));
    }

    private BackendServerResponse mapToResponse(BackendServer s) {
        return BackendServerResponse.builder()
                .id(s.getId())
                .websiteId(s.getWebsiteId())
                .serverName(s.getServerName())
                .privateIp(s.getPrivateIp())
                .publicIp(s.getPublicIp())
                .port(s.getPort())
                .protocol(s.getProtocol())
                .weight(s.getWeight())
                .priority(s.getPriority())
                .maxConnections(s.getMaxConnections())
                .currentConnections(s.getCurrentConnections())
                .cpuUsage(s.getCpuUsage())
                .memoryUsage(s.getMemoryUsage())
                .diskUsage(s.getDiskUsage())
                .healthStatus(s.getHealthStatus())
                .serverStatus(s.getServerStatus())
                .responseTimeMs(s.getResponseTimeMs())
                .lastHealthCheck(s.getLastHealthCheck())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}
