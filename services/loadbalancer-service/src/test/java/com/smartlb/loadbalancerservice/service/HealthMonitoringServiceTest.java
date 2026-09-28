package com.smartlb.loadbalancerservice.service;

import com.smartlb.loadbalancerservice.dto.response.InstanceHealthResponse;
import com.smartlb.loadbalancerservice.entity.ApplicationInstance;
import com.smartlb.loadbalancerservice.entity.HealthStatus;
import com.smartlb.loadbalancerservice.entity.InstanceStatus;
import com.smartlb.loadbalancerservice.exception.ResourceNotFoundException;
import com.smartlb.loadbalancerservice.mapper.ApplicationInstanceMapper;
import com.smartlb.loadbalancerservice.repository.ApplicationInstanceRepository;
import com.smartlb.loadbalancerservice.service.impl.HealthMonitoringServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HealthMonitoringServiceTest {

    @Mock
    private ApplicationInstanceRepository instanceRepository;

    @Mock
    private RestTemplate restTemplate;

    @Spy
    private ApplicationInstanceMapper instanceMapper = Mappers.getMapper(ApplicationInstanceMapper.class);

    private HealthMonitoringServiceImpl healthService;

    private UUID organizationId;
    private UUID instanceId;
    private ApplicationInstance testInstance;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        instanceId = UUID.randomUUID();

        healthService = new HealthMonitoringServiceImpl(
                instanceRepository,
                instanceMapper,
                restTemplate,
                3, // failure threshold
                2000,
                true // enabled
        );

        testInstance = ApplicationInstance.builder()
                .organizationId(organizationId)
                .name("srv-alpha")
                .host("10.0.0.1")
                .port(8080)
                .baseUrl("http://10.0.0.1:8080")
                .healthCheckEndpoint("/health")
                .status(InstanceStatus.ACTIVE)
                .healthStatus(HealthStatus.UNKNOWN)
                .weight(1)
                .currentConnections(0)
                .consecutiveFailures(0)
                .build();
        testInstance.setId(instanceId);
    }

    @Test
    @DisplayName("Should mark instance as HEALTHY when endpoint responds 200 OK")
    void testSuccessfulHealthCheck() {
        when(instanceRepository.findAllByStatusAndDeletedAtIsNull(InstanceStatus.ACTIVE))
                .thenReturn(List.of(testInstance));
        when(restTemplate.getForEntity("http://10.0.0.1:8080/health", String.class))
                .thenReturn(new ResponseEntity<>("UP", HttpStatus.OK));

        healthService.performScheduledHealthChecks();

        assertEquals(HealthStatus.HEALTHY, testInstance.getHealthStatus());
        assertEquals(0, testInstance.getConsecutiveFailures());
        assertNotNull(testInstance.getLastHealthCheck());
        verify(instanceRepository).save(testInstance);
    }

    @Test
    @DisplayName("Should increment consecutive failures when check fails below threshold")
    void testFailureBelowThreshold() {
        when(instanceRepository.findAllByStatusAndDeletedAtIsNull(InstanceStatus.ACTIVE))
                .thenReturn(List.of(testInstance));
        when(restTemplate.getForEntity("http://10.0.0.1:8080/health", String.class))
                .thenThrow(new ResourceAccessException("Connection timed out"));

        healthService.performScheduledHealthChecks();

        assertEquals(1, testInstance.getConsecutiveFailures());
        assertEquals(HealthStatus.UNKNOWN, testInstance.getHealthStatus());
        verify(instanceRepository).save(testInstance);
    }

    @Test
    @DisplayName("Should mark instance as UNHEALTHY when failure threshold is reached")
    void testFailureReachesThreshold() {
        testInstance.setConsecutiveFailures(2);
        when(instanceRepository.findAllByStatusAndDeletedAtIsNull(InstanceStatus.ACTIVE))
                .thenReturn(List.of(testInstance));
        when(restTemplate.getForEntity("http://10.0.0.1:8080/health", String.class))
                .thenReturn(new ResponseEntity<>("Internal Error", HttpStatus.INTERNAL_SERVER_ERROR));

        healthService.performScheduledHealthChecks();

        assertEquals(3, testInstance.getConsecutiveFailures());
        assertEquals(HealthStatus.UNHEALTHY, testInstance.getHealthStatus());
        verify(instanceRepository).save(testInstance);
    }

    @Test
    @DisplayName("Should not execute checks when health monitoring is disabled")
    void testDisabledHealthChecks() {
        HealthMonitoringServiceImpl disabledService = new HealthMonitoringServiceImpl(
                instanceRepository,
                instanceMapper,
                restTemplate,
                3,
                2000,
                false
        );

        disabledService.performScheduledHealthChecks();

        verifyNoInteractions(instanceRepository);
        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("Should return instance health response")
    void testGetSingleInstanceHealthSuccess() {
        when(instanceRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(instanceId, organizationId))
                .thenReturn(Optional.of(testInstance));

        InstanceHealthResponse response = healthService.getSingleInstanceHealth(organizationId, instanceId);

        assertNotNull(response);
        assertEquals(instanceId, response.getInstanceId());
        assertEquals(HealthStatus.UNKNOWN, response.getHealthStatus());
        assertEquals(0, response.getConsecutiveFailures());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when instance does not exist")
    void testGetSingleInstanceHealthNotFoundThrows() {
        UUID unknownId = UUID.randomUUID();
        when(instanceRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(unknownId, organizationId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> healthService.getSingleInstanceHealth(organizationId, unknownId));
    }
}
