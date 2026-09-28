package com.smartlb.loadbalancerservice.service;

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
import com.smartlb.loadbalancerservice.security.ssrf.SsrfValidator;
import com.smartlb.loadbalancerservice.service.impl.ApplicationInstanceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationInstanceServiceTest {

    @Mock
    private ApplicationInstanceRepository instanceRepository;

    @Spy
    private ApplicationInstanceMapper instanceMapper = Mappers.getMapper(ApplicationInstanceMapper.class);

    @Mock
    private SsrfValidator ssrfValidator;

    @InjectMocks
    private ApplicationInstanceServiceImpl instanceService;

    private UUID organizationId;
    private UUID instanceId;
    private ApplicationInstance existingInstance;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        instanceId = UUID.randomUUID();

        existingInstance = ApplicationInstance.builder()
                .organizationId(organizationId)
                .name("backend-srv-1")
                .host("10.0.0.1")
                .port(8080)
                .baseUrl("http://10.0.0.1:8080")
                .healthCheckEndpoint("/actuator/health")
                .status(InstanceStatus.ACTIVE)
                .healthStatus(HealthStatus.HEALTHY)
                .weight(1)
                .currentConnections(0)
                .consecutiveFailures(0)
                .build();
        existingInstance.setId(instanceId);

        // Allow SSRF validation to pass by default for all tests
        doNothing().when(ssrfValidator).validateTarget(any(), any(Integer.class));
    }


    @Test
    @DisplayName("Should successfully register a new application instance")
    void testRegisterInstanceSuccess() {
        RegisterInstanceRequest request = RegisterInstanceRequest.builder()
                .name("backend-srv-2")
                .host("10.0.0.2")
                .port(8080)
                .healthCheckEndpoint("health")
                .weight(2)
                .build();

        when(instanceRepository.existsByNameAndOrganizationIdAndDeletedAtIsNull(request.getName(), organizationId)).thenReturn(false);
        when(instanceRepository.existsByHostAndPortAndOrganizationIdAndDeletedAtIsNull(request.getHost(), request.getPort(), organizationId)).thenReturn(false);
        when(instanceRepository.save(any(ApplicationInstance.class))).thenAnswer(invocation -> {
            ApplicationInstance toSave = invocation.getArgument(0);
            toSave.setId(UUID.randomUUID());
            return toSave;
        });

        InstanceResponse response = instanceService.registerInstance(organizationId, request);

        assertNotNull(response);
        assertEquals("backend-srv-2", response.getName());
        assertEquals("http://10.0.0.2:8080", response.getBaseUrl());
        assertEquals("/health", response.getHealthCheckEndpoint());
        assertEquals(InstanceStatus.ACTIVE, response.getStatus());
        assertEquals(HealthStatus.UNKNOWN, response.getHealthStatus());
        verify(instanceRepository).save(any(ApplicationInstance.class));
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when instance name already exists for organization")
    void testRegisterDuplicateNameThrows() {
        RegisterInstanceRequest request = RegisterInstanceRequest.builder()
                .name("backend-srv-1")
                .host("10.0.0.5")
                .port(9000)
                .build();

        when(instanceRepository.existsByNameAndOrganizationIdAndDeletedAtIsNull("backend-srv-1", organizationId)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> instanceService.registerInstance(organizationId, request));
        verify(instanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when instance host and port already exist for organization")
    void testRegisterDuplicateHostPortThrows() {
        RegisterInstanceRequest request = RegisterInstanceRequest.builder()
                .name("backend-srv-unique")
                .host("10.0.0.1")
                .port(8080)
                .build();

        when(instanceRepository.existsByNameAndOrganizationIdAndDeletedAtIsNull("backend-srv-unique", organizationId)).thenReturn(false);
        when(instanceRepository.existsByHostAndPortAndOrganizationIdAndDeletedAtIsNull("10.0.0.1", 8080, organizationId)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> instanceService.registerInstance(organizationId, request));
        verify(instanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should get instance by ID successfully")
    void testGetInstanceSuccess() {
        when(instanceRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(instanceId, organizationId))
                .thenReturn(Optional.of(existingInstance));

        InstanceResponse response = instanceService.getInstance(organizationId, instanceId);

        assertNotNull(response);
        assertEquals(instanceId, response.getId());
        assertEquals("backend-srv-1", response.getName());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when instance does not exist")
    void testGetInstanceNotFoundThrows() {
        UUID unknownId = UUID.randomUUID();
        when(instanceRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(unknownId, organizationId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> instanceService.getInstance(organizationId, unknownId));
    }

    @Test
    @DisplayName("Should list all tenant instances")
    void testListTenantInstances() {
        when(instanceRepository.findAllByOrganizationIdAndDeletedAtIsNull(organizationId))
                .thenReturn(List.of(existingInstance));

        List<InstanceResponse> responses = instanceService.listTenantInstances(organizationId);

        assertEquals(1, responses.size());
        assertEquals("backend-srv-1", responses.get(0).getName());
    }

    @Test
    @DisplayName("Should update instance successfully")
    void testUpdateInstanceSuccess() {
        UpdateInstanceRequest request = UpdateInstanceRequest.builder()
                .name("backend-srv-updated")
                .host("10.0.0.10")
                .port(8081)
                .healthCheckEndpoint("/status")
                .weight(5)
                .build();

        when(instanceRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(instanceId, organizationId))
                .thenReturn(Optional.of(existingInstance));
        when(instanceRepository.existsByNameAndOrganizationIdAndDeletedAtIsNull(request.getName(), organizationId))
                .thenReturn(false);
        when(instanceRepository.existsByHostAndPortAndOrganizationIdAndDeletedAtIsNull(request.getHost(), request.getPort(), organizationId))
                .thenReturn(false);
        when(instanceRepository.save(any(ApplicationInstance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InstanceResponse response = instanceService.updateInstance(organizationId, instanceId, request);

        assertNotNull(response);
        assertEquals("backend-srv-updated", response.getName());
        assertEquals(8081, response.getPort());
        assertEquals(5, response.getWeight());
        assertEquals("/status", response.getHealthCheckEndpoint());
    }

    @Test
    @DisplayName("Should update instance status")
    void testUpdateInstanceStatus() {
        when(instanceRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(instanceId, organizationId))
                .thenReturn(Optional.of(existingInstance));
        when(instanceRepository.save(any(ApplicationInstance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InstanceResponse response = instanceService.updateInstanceStatus(organizationId, instanceId, InstanceStatus.DRAINING);

        assertEquals(InstanceStatus.DRAINING, response.getStatus());
    }

    @Test
    @DisplayName("Should delete instance by marking decommissioned and setting deletedAt")
    void testDeleteInstance() {
        when(instanceRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(instanceId, organizationId))
                .thenReturn(Optional.of(existingInstance));

        instanceService.deleteInstance(organizationId, instanceId);

        assertEquals(InstanceStatus.DECOMMISSIONED, existingInstance.getStatus());
        assertNotNull(existingInstance.getDeletedAt());
        verify(instanceRepository).save(existingInstance);
    }

    @Test
    @DisplayName("Should retrieve healthy instances only")
    void testGetHealthyInstances() {
        when(instanceRepository.findHealthyInstancesByOrganizationId(organizationId))
                .thenReturn(List.of(existingInstance));

        List<InstanceResponse> healthy = instanceService.getHealthyInstances(organizationId);

        assertEquals(1, healthy.size());
        assertEquals(HealthStatus.HEALTHY, healthy.get(0).getHealthStatus());
    }
}
