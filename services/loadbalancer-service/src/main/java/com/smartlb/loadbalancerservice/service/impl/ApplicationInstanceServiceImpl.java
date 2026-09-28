package com.smartlb.loadbalancerservice.service.impl;

import com.smartlb.loadbalancerservice.dto.request.RegisterInstanceRequest;
import com.smartlb.loadbalancerservice.dto.request.UpdateInstanceRequest;
import com.smartlb.loadbalancerservice.dto.response.InstanceResponse;
import com.smartlb.loadbalancerservice.entity.ApplicationInstance;
import com.smartlb.loadbalancerservice.entity.HealthStatus;
import com.smartlb.loadbalancerservice.entity.InstanceStatus;
import com.smartlb.loadbalancerservice.exception.DuplicateResourceException;
import com.smartlb.loadbalancerservice.exception.ResourceNotFoundException;
import com.smartlb.loadbalancerservice.mapper.ApplicationInstanceMapper;
import com.smartlb.loadbalancerservice.repository.ApplicationInstanceRepository;
import com.smartlb.loadbalancerservice.service.ApplicationInstanceService;
import com.smartlb.loadbalancerservice.security.ssrf.SsrfValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of {@link ApplicationInstanceService} handling instance registration,
 * tenant isolation, updates, status transitions, and decommission logic.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationInstanceServiceImpl implements ApplicationInstanceService {

    private final ApplicationInstanceRepository instanceRepository;
    private final ApplicationInstanceMapper instanceMapper;
    private final SsrfValidator ssrfValidator;

    @Override
    @Transactional
    public InstanceResponse registerInstance(UUID organizationId, RegisterInstanceRequest request) {
        log.info("Registering application instance '{}' for organization ID: {}", request.getName(), organizationId);

        ssrfValidator.validateTarget(request.getHost(), request.getPort());

        if (instanceRepository.existsByNameAndOrganizationIdAndDeletedAtIsNull(request.getName(), organizationId)) {
            throw new DuplicateResourceException("Instance with name '" + request.getName() + "' already exists for this organization");
        }

        if (instanceRepository.existsByHostAndPortAndOrganizationIdAndDeletedAtIsNull(request.getHost(), request.getPort(), organizationId)) {
            throw new DuplicateResourceException("Instance with host '" + request.getHost() + "' and port " + request.getPort() + " already exists for this organization");
        }

        String healthEndpoint = StringUtils.hasText(request.getHealthCheckEndpoint()) ? request.getHealthCheckEndpoint() : "/health";
        if (!healthEndpoint.startsWith("/")) {
            healthEndpoint = "/" + healthEndpoint;
        }

        String computedBaseUrl = StringUtils.hasText(request.getBaseUrl())
                ? request.getBaseUrl()
                : "http://" + request.getHost() + ":" + request.getPort();

        ApplicationInstance instance = ApplicationInstance.builder()
                .organizationId(organizationId)
                .name(request.getName())
                .host(request.getHost())
                .port(request.getPort())
                .baseUrl(computedBaseUrl)
                .healthCheckEndpoint(healthEndpoint)
                .status(InstanceStatus.ACTIVE)
                .healthStatus(HealthStatus.UNKNOWN)
                .weight(request.getWeight() != null ? request.getWeight() : 1)
                .currentConnections(0)
                .consecutiveFailures(0)
                .build();

        ApplicationInstance savedInstance = instanceRepository.save(instance);
        log.info("Successfully registered instance ID: {} for organization ID: {}", savedInstance.getId(), organizationId);
        return instanceMapper.toResponse(savedInstance);
    }

    @Override
    @Transactional(readOnly = true)
    public InstanceResponse getInstance(UUID organizationId, UUID instanceId) {
        ApplicationInstance instance = findInstanceOrThrow(instanceId, organizationId);
        return instanceMapper.toResponse(instance);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstanceResponse> listTenantInstances(UUID organizationId) {
        List<ApplicationInstance> instances = instanceRepository.findAllByOrganizationIdAndDeletedAtIsNull(organizationId);
        return instanceMapper.toResponseList(instances);
    }

    @Override
    @Transactional
    public InstanceResponse updateInstance(UUID organizationId, UUID instanceId, UpdateInstanceRequest request) {
        log.info("Updating instance ID: {} for organization ID: {}", instanceId, organizationId);

        ApplicationInstance instance = findInstanceOrThrow(instanceId, organizationId);

        if (!instance.getName().equals(request.getName()) &&
            instanceRepository.existsByNameAndOrganizationIdAndDeletedAtIsNull(request.getName(), organizationId)) {
            throw new DuplicateResourceException("Instance with name '" + request.getName() + "' already exists for this organization");
        }

        if ((!instance.getHost().equals(request.getHost()) || !instance.getPort().equals(request.getPort())) &&
            instanceRepository.existsByHostAndPortAndOrganizationIdAndDeletedAtIsNull(request.getHost(), request.getPort(), organizationId)) {
            throw new DuplicateResourceException("Instance with host '" + request.getHost() + "' and port " + request.getPort() + " already exists for this organization");
        }

        String healthEndpoint = StringUtils.hasText(request.getHealthCheckEndpoint()) ? request.getHealthCheckEndpoint() : "/health";
        if (!healthEndpoint.startsWith("/")) {
            healthEndpoint = "/" + healthEndpoint;
        }

        String computedBaseUrl = StringUtils.hasText(request.getBaseUrl())
                ? request.getBaseUrl()
                : "http://" + request.getHost() + ":" + request.getPort();

        instance.setName(request.getName());
        instance.setHost(request.getHost());
        instance.setPort(request.getPort());
        instance.setBaseUrl(computedBaseUrl);
        instance.setHealthCheckEndpoint(healthEndpoint);
        if (request.getWeight() != null) {
            instance.setWeight(request.getWeight());
        }

        ApplicationInstance updatedInstance = instanceRepository.save(instance);
        log.info("Successfully updated instance ID: {}", updatedInstance.getId());
        return instanceMapper.toResponse(updatedInstance);
    }

    @Override
    @Transactional
    public InstanceResponse updateInstanceStatus(UUID organizationId, UUID instanceId, InstanceStatus status) {
        log.info("Updating status for instance ID: {} to {} for organization ID: {}", instanceId, status, organizationId);

        ApplicationInstance instance = findInstanceOrThrow(instanceId, organizationId);
        instance.setStatus(status);

        ApplicationInstance updatedInstance = instanceRepository.save(instance);
        return instanceMapper.toResponse(updatedInstance);
    }

    @Override
    @Transactional
    public void deleteInstance(UUID organizationId, UUID instanceId) {
        log.info("Decommissioning/Deleting instance ID: {} for organization ID: {}", instanceId, organizationId);

        ApplicationInstance instance = findInstanceOrThrow(instanceId, organizationId);
        instance.setStatus(InstanceStatus.DECOMMISSIONED);
        instance.setDeletedAt(Instant.now());
        instanceRepository.save(instance);

        log.info("Successfully deleted instance ID: {}", instanceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstanceResponse> getHealthyInstances(UUID organizationId) {
        List<ApplicationInstance> healthyInstances = instanceRepository.findHealthyInstancesByOrganizationId(organizationId);
        return instanceMapper.toResponseList(healthyInstances);
    }

    private ApplicationInstance findInstanceOrThrow(UUID instanceId, UUID organizationId) {
        return instanceRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(instanceId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application instance with ID " + instanceId + " not found"));
    }
}
