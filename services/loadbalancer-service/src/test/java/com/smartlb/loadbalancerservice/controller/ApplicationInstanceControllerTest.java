package com.smartlb.loadbalancerservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlb.loadbalancerservice.dto.request.RegisterInstanceRequest;
import com.smartlb.loadbalancerservice.dto.request.UpdateInstanceRequest;
import com.smartlb.loadbalancerservice.dto.request.UpdateStatusRequest;
import com.smartlb.loadbalancerservice.dto.response.InstanceHealthResponse;
import com.smartlb.loadbalancerservice.dto.response.InstanceResponse;
import com.smartlb.loadbalancerservice.entity.HealthStatus;
import com.smartlb.loadbalancerservice.entity.InstanceStatus;
import com.smartlb.loadbalancerservice.exception.DuplicateResourceException;
import com.smartlb.loadbalancerservice.exception.ResourceNotFoundException;
import com.smartlb.loadbalancerservice.security.UserPrincipal;
import com.smartlb.loadbalancerservice.service.ApplicationInstanceService;
import com.smartlb.loadbalancerservice.service.HealthMonitoringService;
import com.smartlb.loadbalancerservice.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ApplicationInstanceController.class)
@AutoConfigureMockMvc(addFilters = false)
class ApplicationInstanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ApplicationInstanceService instanceService;

    @MockBean
    private HealthMonitoringService healthMonitoringService;

    @MockBean
    private JwtService jwtService;

    private UUID organizationId;
    private UUID instanceId;
    private UserPrincipal principal;
    private UsernamePasswordAuthenticationToken auth;
    private InstanceResponse sampleResponse;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        instanceId = UUID.randomUUID();

        principal = UserPrincipal.createFromClaims(
                UUID.randomUUID(),
                organizationId,
                "admin@smartlb.io",
                List.of("ROLE_ORG_ADMIN")
        );

        auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        sampleResponse = InstanceResponse.builder()
                .id(instanceId)
                .name("srv-1")
                .host("10.0.0.1")
                .port(8080)
                .baseUrl("http://10.0.0.1:8080")
                .healthCheckEndpoint("/health")
                .status(InstanceStatus.ACTIVE)
                .healthStatus(HealthStatus.HEALTHY)
                .weight(1)
                .currentConnections(0)
                .consecutiveFailures(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("POST /api/instances - successfully registers instance")
    void testRegisterInstanceSuccess() throws Exception {
        RegisterInstanceRequest request = RegisterInstanceRequest.builder()
                .name("srv-1")
                .host("10.0.0.1")
                .port(8080)
                .healthCheckEndpoint("/health")
                .weight(1)
                .build();

        when(instanceService.registerInstance(eq(organizationId), any(RegisterInstanceRequest.class)))
                .thenReturn(sampleResponse);

        mockMvc.perform(post("/api/instances")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", is("srv-1")))
                .andExpect(jsonPath("$.data.port", is(8080)));
    }

    @Test
    @DisplayName("POST /api/instances - returns 400 Bad Request on invalid payload")
    void testRegisterInstanceInvalidPayload() throws Exception {
        RegisterInstanceRequest invalidRequest = RegisterInstanceRequest.builder()
                .name("")
                .host("")
                .port(-5)
                .build();

        mockMvc.perform(post("/api/instances")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    @DisplayName("POST /api/instances - returns 409 Conflict on duplicate resource")
    void testRegisterInstanceDuplicate() throws Exception {
        RegisterInstanceRequest request = RegisterInstanceRequest.builder()
                .name("srv-1")
                .host("10.0.0.1")
                .port(8080)
                .build();

        when(instanceService.registerInstance(eq(organizationId), any(RegisterInstanceRequest.class)))
                .thenThrow(new DuplicateResourceException("Instance already exists"));

        mockMvc.perform(post("/api/instances")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", is("Instance already exists")));
    }

    @Test
    @DisplayName("GET /api/instances - returns list of instances")
    void testListInstances() throws Exception {
        when(instanceService.listTenantInstances(organizationId))
                .thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/instances")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data[0].id", is(instanceId.toString())));
    }

    @Test
    @DisplayName("GET /api/instances/healthy - returns healthy instances")
    void testGetHealthyInstances() throws Exception {
        when(instanceService.getHealthyInstances(organizationId))
                .thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/instances/healthy")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data[0].healthStatus", is("HEALTHY")));
    }

    @Test
    @DisplayName("GET /api/instances/{id} - returns instance details")
    void testGetInstanceFound() throws Exception {
        when(instanceService.getInstance(organizationId, instanceId))
                .thenReturn(sampleResponse);

        mockMvc.perform(get("/api/instances/" + instanceId)
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(instanceId.toString())));
    }

    @Test
    @DisplayName("GET /api/instances/{id} - returns 404 when not found")
    void testGetInstanceNotFound() throws Exception {
        UUID unknownId = UUID.randomUUID();
        when(instanceService.getInstance(organizationId, unknownId))
                .thenThrow(new ResourceNotFoundException("Application instance not found"));

        mockMvc.perform(get("/api/instances/" + unknownId)
                        .with(authentication(auth)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    @DisplayName("PUT /api/instances/{id} - updates instance")
    void testUpdateInstance() throws Exception {
        UpdateInstanceRequest request = UpdateInstanceRequest.builder()
                .name("srv-renamed")
                .host("10.0.0.2")
                .port(8081)
                .build();

        when(instanceService.updateInstance(eq(organizationId), eq(instanceId), any(UpdateInstanceRequest.class)))
                .thenReturn(sampleResponse);

        mockMvc.perform(put("/api/instances/" + instanceId)
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    @DisplayName("PATCH /api/instances/{id}/status - updates instance status")
    void testUpdateInstanceStatus() throws Exception {
        UpdateStatusRequest request = new UpdateStatusRequest();
        request.setStatus(InstanceStatus.DRAINING);

        when(instanceService.updateInstanceStatus(eq(organizationId), eq(instanceId), eq(InstanceStatus.DRAINING)))
                .thenReturn(sampleResponse);

        mockMvc.perform(patch("/api/instances/" + instanceId + "/status")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    @DisplayName("DELETE /api/instances/{id} - deletes instance")
    void testDeleteInstance() throws Exception {
        doNothing().when(instanceService).deleteInstance(organizationId, instanceId);

        mockMvc.perform(delete("/api/instances/" + instanceId)
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    @DisplayName("GET /api/instances/{id}/health - retrieves health status")
    void testGetInstanceHealth() throws Exception {
        InstanceHealthResponse healthResponse = InstanceHealthResponse.builder()
                .instanceId(instanceId)
                .name("srv-1")
                .baseUrl("http://10.0.0.1:8080")
                .healthStatus(HealthStatus.HEALTHY)
                .consecutiveFailures(0)
                .lastHealthCheck(Instant.now())
                .build();

        when(healthMonitoringService.getSingleInstanceHealth(organizationId, instanceId))
                .thenReturn(healthResponse);

        mockMvc.perform(get("/api/instances/" + instanceId + "/health")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.healthStatus", is("HEALTHY")));
    }
}
