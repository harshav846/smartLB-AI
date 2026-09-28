package com.smartlb.customerservice.service;

import com.smartlb.customerservice.dto.request.LoadBalancerConfigRequest;
import com.smartlb.customerservice.dto.response.LoadBalancerConfigResponse;

import java.util.UUID;

public interface LoadBalancerConfigService {

    LoadBalancerConfigResponse getConfig(UUID organizationId, UUID websiteId);

    LoadBalancerConfigResponse updateConfig(UUID organizationId, UUID websiteId, UUID userId, LoadBalancerConfigRequest request);
}
