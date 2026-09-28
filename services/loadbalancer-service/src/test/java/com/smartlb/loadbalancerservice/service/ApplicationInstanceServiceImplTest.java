package com.smartlb.loadbalancerservice.service;

import com.smartlb.loadbalancerservice.dto.request.RegisterInstanceRequest;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ApplicationInstanceServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class ApplicationInstanceServiceImplTest {

    @Mock
    private ApplicationInstanceRepository instanceRepository;

    @Mock
    private ApplicationInstanceMapper instanceMapper;

    @Mock
    private SsrfValidator ssrfValidator;

    private ApplicationInstanceService service;

    private final UUID ORG_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ApplicationInstanceServiceImpl(instanceRepository, instanceMapper, ssrfValidator);
    }

    private ApplicationInstance buildInstance(UUID id, String name) {
        return ApplicationInstance.builder()
                .organizationId(ORG_ID)
                .name(name)
                .host("localhost")
                .port(8080)
                .baseUrl("http://localhost:8080")
                .healthCheckEndpoint("/health")
                .status(InstanceStatus.ACTIVE)
                .healthStatus(HealthStatus.HEALTHY)
                .weight(1)
                .currentConnections(0)
                .consecutiveFailures(0)
                .build();
    }

    private RegisterInstanceRequest buildRegisterRequest(String name) {
        RegisterInstanceRequest req = new RegisterInstanceRequest();
        req.setName(name);
        req.setHost("localhost");
        req.setPort(8080);
        req.setHealthCheckEndpoint("/health");
        return req;
    }

    /* ── registerInstance ───────────────────────────────────────────── */

    @Test
    void registerInstance_success() {
        RegisterInstanceRequest req = buildRegisterRequest("server-1");

        doNothing().when(ssrfValidator).validateTarget(any(), any(Integer.class));
        when(instanceRepository.existsByNameAndOrganizationIdAndDeletedAtIsNull(any(), any())).thenReturn(false);
        when(instanceRepository.existsByHostAndPortAndOrganizationIdAndDeletedAtIsNull(any(), any(), any())).thenReturn(false);
        ApplicationInstance saved = buildInstance(UUID.randomUUID(), "server-1");
        when(instanceRepository.save(any())).thenReturn(saved);
        InstanceResponse mockResponse = new InstanceResponse();
        when(instanceMapper.toResponse(any())).thenReturn(mockResponse);

        InstanceResponse response = service.registerInstance(ORG_ID, req);
        assertThat(response).isNotNull();
        verify(instanceRepository).save(any());
    }

    @Test
    void registerInstance_duplicateName_throwsDuplicateResourceException() {
        RegisterInstanceRequest req = buildRegisterRequest("dup-server");

        doNothing().when(ssrfValidator).validateTarget(any(), any(Integer.class));
        when(instanceRepository.existsByNameAndOrganizationIdAndDeletedAtIsNull("dup-server", ORG_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.registerInstance(ORG_ID, req))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("dup-server");
    }

    @Test
    void registerInstance_duplicateHostPort_throwsDuplicateResourceException() {
        RegisterInstanceRequest req = buildRegisterRequest("new-name");

        doNothing().when(ssrfValidator).validateTarget(any(), any(Integer.class));
        when(instanceRepository.existsByNameAndOrganizationIdAndDeletedAtIsNull(any(), any())).thenReturn(false);
        when(instanceRepository.existsByHostAndPortAndOrganizationIdAndDeletedAtIsNull("localhost", 8080, ORG_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.registerInstance(ORG_ID, req))
                .isInstanceOf(DuplicateResourceException.class);
    }

    /* ── getInstance ────────────────────────────────────────────────── */

    @Test
    void getInstance_notFound_throwsResourceNotFoundException() {
        UUID instanceId = UUID.randomUUID();
        when(instanceRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(instanceId, ORG_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getInstance(ORG_ID, instanceId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getInstance_found_returnsResponse() {
        UUID instanceId = UUID.randomUUID();
        ApplicationInstance instance = buildInstance(instanceId, "found-server");
        when(instanceRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(instanceId, ORG_ID))
                .thenReturn(Optional.of(instance));
        InstanceResponse mockResponse = new InstanceResponse();
        when(instanceMapper.toResponse(instance)).thenReturn(mockResponse);

        InstanceResponse response = service.getInstance(ORG_ID, instanceId);
        assertThat(response).isNotNull();
    }

    /* ── listTenantInstances ────────────────────────────────────────── */

    @Test
    void listTenantInstances_returnsAllForOrg() {
        ApplicationInstance i1 = buildInstance(UUID.randomUUID(), "s1");
        ApplicationInstance i2 = buildInstance(UUID.randomUUID(), "s2");
        when(instanceRepository.findAllByOrganizationIdAndDeletedAtIsNull(ORG_ID))
                .thenReturn(List.of(i1, i2));
        when(instanceMapper.toResponseList(any())).thenReturn(List.of(new InstanceResponse(), new InstanceResponse()));

        List<InstanceResponse> result = service.listTenantInstances(ORG_ID);
        assertThat(result).hasSize(2);
    }

    /* ── deleteInstance ─────────────────────────────────────────────── */

    @Test
    void deleteInstance_setsDecommissionedAndDeletedAt() {
        UUID instanceId = UUID.randomUUID();
        ApplicationInstance instance = buildInstance(instanceId, "to-delete");
        when(instanceRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(instanceId, ORG_ID))
                .thenReturn(Optional.of(instance));
        when(instanceRepository.save(any())).thenReturn(instance);

        service.deleteInstance(ORG_ID, instanceId);

        assertThat(instance.getStatus()).isEqualTo(InstanceStatus.DECOMMISSIONED);
        assertThat(instance.getDeletedAt()).isNotNull();
    }

    /* ── getHealthyInstances ────────────────────────────────────────── */

    @Test
    void getHealthyInstances_returnsOnlyHealthy() {
        ApplicationInstance healthy = buildInstance(UUID.randomUUID(), "healthy");
        when(instanceRepository.findHealthyInstancesByOrganizationId(ORG_ID))
                .thenReturn(List.of(healthy));
        when(instanceMapper.toResponseList(any())).thenReturn(List.of(new InstanceResponse()));

        List<InstanceResponse> result = service.getHealthyInstances(ORG_ID);
        assertThat(result).hasSize(1);
    }
}
