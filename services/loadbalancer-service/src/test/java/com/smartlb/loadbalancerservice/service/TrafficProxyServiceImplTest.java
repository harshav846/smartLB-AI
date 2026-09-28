package com.smartlb.loadbalancerservice.service;

import com.smartlb.loadbalancerservice.algorithm.LoadBalancingAlgorithm;
import com.smartlb.loadbalancerservice.algorithm.LoadBalancingStrategyFactory;
import com.smartlb.loadbalancerservice.algorithm.impl.RoundRobinStrategy;
import com.smartlb.loadbalancerservice.dto.response.ProxyResponse;
import com.smartlb.loadbalancerservice.entity.ApplicationInstance;
import com.smartlb.loadbalancerservice.entity.HealthStatus;
import com.smartlb.loadbalancerservice.entity.InstanceStatus;
import com.smartlb.loadbalancerservice.exception.ResourceNotFoundException;
import com.smartlb.loadbalancerservice.repository.ApplicationInstanceRepository;
import com.smartlb.loadbalancerservice.service.impl.TrafficProxyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TrafficProxyServiceImpl}.
 * RestTemplate is mocked so no real HTTP requests are made.
 */
@ExtendWith(MockitoExtension.class)
class TrafficProxyServiceImplTest {

    @Mock
    private ApplicationInstanceRepository instanceRepository;

    @Mock
    private RestTemplate restTemplate;

    private TrafficProxyServiceImpl proxyService;

    private final UUID ORG_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        RoundRobinStrategy rrStrategy = new RoundRobinStrategy();
        LoadBalancingStrategyFactory factory = new LoadBalancingStrategyFactory(List.of(rrStrategy));
        proxyService = new TrafficProxyServiceImpl(instanceRepository, factory, restTemplate);
    }

    private ApplicationInstance healthyInstance(String name) {
        return ApplicationInstance.builder()
                .organizationId(ORG_ID)
                .name(name)
                .host("10.0.0.1")
                .port(8080)
                .baseUrl("http://10.0.0.1:8080")
                .status(InstanceStatus.ACTIVE)
                .healthStatus(HealthStatus.HEALTHY)
                .weight(1)
                .currentConnections(0)
                .consecutiveFailures(0)
                .build();
    }

    /* ── proxy success ──────────────────────────────────────────────── */

    @Test
    void proxy_success_returnsProxyResponse() {
        ApplicationInstance inst = healthyInstance("server-1");
        when(instanceRepository.findHealthyInstancesByOrganizationId(ORG_ID))
                .thenReturn(List.of(inst));
        when(instanceRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"status\":\"ok\"}"));

        ProxyResponse response = proxyService.proxy(
                ORG_ID, "GET", "/api/test", null, Map.of(), "1.2.3.4", LoadBalancingAlgorithm.ROUND_ROBIN);

        assertThat(response.getHttpStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.getResponseBody()).contains("ok");
        assertThat(response.getInstanceId()).isEqualTo(inst.getId());
        assertThat(response.isRetried()).isFalse();
    }

    @Test
    void proxy_noHealthyInstances_throwsResourceNotFoundException() {
        when(instanceRepository.findHealthyInstancesByOrganizationId(ORG_ID))
                .thenReturn(Collections.emptyList());

        assertThatThrownBy(() ->
                proxyService.proxy(ORG_ID, "GET", "/api/test", null, Map.of(), "1.2.3.4", null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No healthy instances available");
    }

    /* ── selectInstance ─────────────────────────────────────────────── */

    @Test
    void selectInstance_returnsSelectedInstance() {
        ApplicationInstance inst = healthyInstance("server-dry");
        when(instanceRepository.findHealthyInstancesByOrganizationId(ORG_ID))
                .thenReturn(List.of(inst));

        ApplicationInstance selected = proxyService.selectInstance(ORG_ID, "1.2.3.4", LoadBalancingAlgorithm.ROUND_ROBIN);
        assertThat(selected).isSameAs(inst);
    }

    @Test
    void selectInstance_noInstances_throwsResourceNotFoundException() {
        when(instanceRepository.findHealthyInstancesByOrganizationId(ORG_ID))
                .thenReturn(Collections.emptyList());

        assertThatThrownBy(() ->
                proxyService.selectInstance(ORG_ID, "1.2.3.4", LoadBalancingAlgorithm.ROUND_ROBIN))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    /* ── target URL construction ────────────────────────────────────── */

    @Test
    void proxy_buildsCorrectTargetUrl() {
        ApplicationInstance inst = healthyInstance("server-url");
        when(instanceRepository.findHealthyInstancesByOrganizationId(ORG_ID))
                .thenReturn(List.of(inst));
        when(instanceRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Capture the URL passed to RestTemplate
        final String[] capturedUrl = {null};
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
                .thenAnswer(invocation -> {
                    capturedUrl[0] = invocation.getArgument(0);
                    return ResponseEntity.ok("{}");
                });

        proxyService.proxy(ORG_ID, "GET", "/api/items?page=1", null, Map.of(), "x", null);

        assertThat(capturedUrl[0]).isEqualTo("http://10.0.0.1:8080/api/items?page=1");
    }
}
