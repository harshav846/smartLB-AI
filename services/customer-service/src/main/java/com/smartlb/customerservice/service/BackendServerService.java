package com.smartlb.customerservice.service;

import com.smartlb.customerservice.dto.request.BackendServerRequest;
import com.smartlb.customerservice.dto.response.BackendServerResponse;

import java.util.List;
import java.util.UUID;

public interface BackendServerService {

    BackendServerResponse registerBackendServer(UUID organizationId, UUID websiteId, UUID userId, BackendServerRequest request);

    List<BackendServerResponse> listBackendServers(UUID organizationId, UUID websiteId);

    BackendServerResponse getBackendServer(UUID organizationId, UUID websiteId, UUID serverId);

    BackendServerResponse updateBackendServer(UUID organizationId, UUID websiteId, UUID serverId, BackendServerRequest request);

    void deleteBackendServer(UUID organizationId, UUID websiteId, UUID serverId);

    BackendServerResponse updateServerStatus(UUID organizationId, UUID websiteId, UUID serverId, String status);
}
