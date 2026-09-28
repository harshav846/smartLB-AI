package com.smartlb.loadbalancerservice.service;

import com.smartlb.loadbalancerservice.dto.request.RegisterInstanceRequest;
import com.smartlb.loadbalancerservice.dto.request.UpdateInstanceRequest;
import com.smartlb.loadbalancerservice.dto.response.InstanceResponse;
import com.smartlb.loadbalancerservice.entity.InstanceStatus;

import java.util.List;
import java.util.UUID;

/**
 * Service interface managing Application Instance lifecycle and registration.
 */
public interface ApplicationInstanceService {

    InstanceResponse registerInstance(UUID organizationId, RegisterInstanceRequest request);

    InstanceResponse getInstance(UUID organizationId, UUID instanceId);

    List<InstanceResponse> listTenantInstances(UUID organizationId);

    InstanceResponse updateInstance(UUID organizationId, UUID instanceId, UpdateInstanceRequest request);

    InstanceResponse updateInstanceStatus(UUID organizationId, UUID instanceId, InstanceStatus status);

    void deleteInstance(UUID organizationId, UUID instanceId);

    List<InstanceResponse> getHealthyInstances(UUID organizationId);
}
