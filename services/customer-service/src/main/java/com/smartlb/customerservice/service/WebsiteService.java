package com.smartlb.customerservice.service;

import com.smartlb.customerservice.dto.request.WebsiteRequest;
import com.smartlb.customerservice.dto.response.WebsiteResponse;

import java.util.List;
import java.util.UUID;

public interface WebsiteService {

    WebsiteResponse createWebsite(UUID organizationId, UUID userId, WebsiteRequest request);

    List<WebsiteResponse> listWebsites(UUID organizationId);

    WebsiteResponse getWebsite(UUID organizationId, UUID websiteId);

    WebsiteResponse updateWebsite(UUID organizationId, UUID websiteId, WebsiteRequest request);

    void deleteWebsite(UUID organizationId, UUID websiteId);
}
