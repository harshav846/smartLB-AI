package com.smartlb.loadbalancerservice.controller;

import com.smartlb.loadbalancerservice.algorithm.LoadBalancingAlgorithm;
import com.smartlb.loadbalancerservice.algorithm.LoadBalancingStrategy;
import com.smartlb.loadbalancerservice.algorithm.LoadBalancingStrategyFactory;
import com.smartlb.loadbalancerservice.entity.ApplicationInstance;
import com.smartlb.loadbalancerservice.entity.HealthStatus;
import com.smartlb.loadbalancerservice.entity.InstanceStatus;
import com.smartlb.loadbalancerservice.repository.ApplicationInstanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransparentProxyControllerTest {

    @Mock
    private ApplicationInstanceRepository instanceRepository;

    @Mock
    private LoadBalancingStrategyFactory strategyFactory;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private TransparentProxyController controller;

    private UUID orgId;
    private ApplicationInstance instance;

    @BeforeEach
    void setUp() {
        orgId = UUID.randomUUID();
        instance = ApplicationInstance.builder()
                .id(UUID.randomUUID())
                .organizationId(orgId)
                .name("backend-srv-1")
                .host("localhost")
                .port(5001)
                .baseUrl("http://localhost:5001")
                .status(InstanceStatus.ACTIVE)
                .healthStatus(HealthStatus.HEALTHY)
                .currentConnections(0)
                .weight(1)
                .build();

        ReflectionTestUtils.setField(controller, "defaultAlgorithm", "ROUND_ROBIN");
    }

    @Test
    @DisplayName("Should return 503 when no healthy instances are available")
    void handleProxy_noHealthyInstances_returns503() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/proxy/test");
        when(instanceRepository.findAllHealthyInstances()).thenReturn(List.of());

        ResponseEntity<byte[]> response = controller.handleProxy(null, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("Should forward request and return upstream response with metadata headers")
    void handleProxy_healthyInstance_forwardsSuccessfully() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/proxy/api/items");
        request.addHeader("X-Organization-Id", orgId.toString());

        when(instanceRepository.findHealthyInstancesByOrganizationId(orgId)).thenReturn(List.of(instance));

        LoadBalancingStrategy mockStrategy = mock(LoadBalancingStrategy.class);
        when(mockStrategy.selectInstance(anyList(), any())).thenReturn(instance);
        when(strategyFactory.getStrategy(LoadBalancingAlgorithm.ROUND_ROBIN)).thenReturn(mockStrategy);

        ResponseEntity<byte[]> upstreamResponse = new ResponseEntity<>(
                "{\"items\":[1,2,3]}".getBytes(), HttpStatus.OK
        );
        when(restTemplate.exchange(eq("http://localhost:5001/api/items"), eq(HttpMethod.GET), any(HttpEntity.class), eq(byte[].class)))
                .thenReturn(upstreamResponse);

        ResponseEntity<byte[]> response = controller.handleProxy(null, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(new String(response.getBody())).isEqualTo("{\"items\":[1,2,3]}");
        assertThat(response.getHeaders().getFirst("X-Backend-Server")).isEqualTo("backend-srv-1");
        assertThat(response.getHeaders().getFirst("X-Response-Time-Ms")).isNotNull();
    }
}
