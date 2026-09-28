package com.smartlb.loadbalancerservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlb.loadbalancerservice.dto.request.RegisterInstanceRequest;
import com.smartlb.loadbalancerservice.dto.request.UpdateInstanceRequest;
import com.smartlb.loadbalancerservice.dto.request.UpdateStatusRequest;
import com.smartlb.loadbalancerservice.entity.ApplicationInstance;
import com.smartlb.loadbalancerservice.entity.HealthStatus;
import com.smartlb.loadbalancerservice.entity.InstanceStatus;
import com.smartlb.loadbalancerservice.repository.ApplicationInstanceRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class LoadbalancerIntegrationTest {

    private static final String TEST_SECRET = "dGhpc2lzYWZha2VzZWNyZXRrZXlmb3J0ZXN0aW5ncHVycG9zZXNvbmx5c21hcnRsYmFpMTIzNA==";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApplicationInstanceRepository instanceRepository;

    private Key signingKey;
    private UUID tenantAId;
    private UUID tenantBId;

    private String tenantAAdminToken;
    private String tenantAViewerToken;
    private String tenantBAdminToken;

    @BeforeEach
    void setUp() {
        signingKey = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        tenantAId = UUID.randomUUID();
        tenantBId = UUID.randomUUID();

        tenantAAdminToken = generateToken(UUID.randomUUID(), tenantAId, "admin@tenant-a.com", List.of("ROLE_ORG_ADMIN"));
        tenantAViewerToken = generateToken(UUID.randomUUID(), tenantAId, "viewer@tenant-a.com", List.of("ROLE_MEMBER"));
        tenantBAdminToken = generateToken(UUID.randomUUID(), tenantBId, "admin@tenant-b.com", List.of("ROLE_ORG_ADMIN"));
    }

    private String generateToken(UUID userId, UUID orgId, String email, List<String> roles) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + 3600000);

        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("organizationId", orgId.toString())
                .claim("email", email)
                .claim("roles", roles)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    @Test
    @DisplayName("Should successfully register instance, list it, and ensure multi-tenant isolation")
    void testRegisterAndTenantIsolation() throws Exception {
        RegisterInstanceRequest requestA = RegisterInstanceRequest.builder()
                .name("backend-server-1")
                .host("192.168.1.101")
                .port(5001)
                .healthCheckEndpoint("/health")
                .weight(2)
                .build();

        // 1. Tenant A registers an instance
        MvcResult resultA = mockMvc.perform(post("/api/instances")
                        .header("Authorization", "Bearer " + tenantAAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestA)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", is("backend-server-1")))
                .andReturn();

        String responseBody = resultA.getResponse().getContentAsString();
        String instanceAId = objectMapper.readTree(responseBody).path("data").path("id").asText();
        assertNotNull(instanceAId);

        // 2. Tenant A lists instances -> sees 1 instance
        mockMvc.perform(get("/api/instances")
                        .header("Authorization", "Bearer " + tenantAAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id", is(instanceAId)));

        // 3. Tenant B lists instances -> sees 0 instances (tenant isolation)
        mockMvc.perform(get("/api/instances")
                        .header("Authorization", "Bearer " + tenantBAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        // 4. Tenant B tries to get Tenant A's instance -> 404 Not Found
        mockMvc.perform(get("/api/instances/" + instanceAId)
                        .header("Authorization", "Bearer " + tenantBAdminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));

        // 5. Tenant B registers an instance with the SAME name ("backend-server-1") -> succeeds because names are scoped per tenant
        RegisterInstanceRequest requestB = RegisterInstanceRequest.builder()
                .name("backend-server-1")
                .host("192.168.2.101")
                .port(5001)
                .build();

        mockMvc.perform(post("/api/instances")
                        .header("Authorization", "Bearer " + tenantBAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestB)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)));

        // 6. Tenant A registering the same name again -> 409 Conflict
        mockMvc.perform(post("/api/instances")
                        .header("Authorization", "Bearer " + tenantAAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestA)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)));
    }

    @Test
    @DisplayName("Should enforce role-based access control (MEMBER/VIEWER cannot register or delete instances)")
    void testRbacRestrictions() throws Exception {
        RegisterInstanceRequest request = RegisterInstanceRequest.builder()
                .name("test-backend")
                .host("10.0.0.1")
                .port(8080)
                .build();

        // Non-admin/operator attempting to register instance -> 403 Forbidden
        mockMvc.perform(post("/api/instances")
                        .header("Authorization", "Bearer " + tenantAViewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should reject unauthenticated requests without JWT")
    void testUnauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/api/instances"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should execute update, status change, and delete lifecycle on instance")
    void testInstanceLifecycle() throws Exception {
        RegisterInstanceRequest createRequest = RegisterInstanceRequest.builder()
                .name("lifecycle-srv")
                .host("192.168.1.200")
                .port(8080)
                .build();

        MvcResult result = mockMvc.perform(post("/api/instances")
                        .header("Authorization", "Bearer " + tenantAAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String instanceId = objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asText();

        // Update instance details
        UpdateInstanceRequest updateRequest = UpdateInstanceRequest.builder()
                .name("lifecycle-srv-updated")
                .host("192.168.1.200")
                .port(8088)
                .healthCheckEndpoint("/actuator/health")
                .weight(10)
                .build();

        mockMvc.perform(put("/api/instances/" + instanceId)
                        .header("Authorization", "Bearer " + tenantAAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name", is("lifecycle-srv-updated")))
                .andExpect(jsonPath("$.data.port", is(8088)))
                .andExpect(jsonPath("$.data.weight", is(10)));

        // Patch status
        UpdateStatusRequest statusRequest = new UpdateStatusRequest();
        statusRequest.setStatus(InstanceStatus.DRAINING);

        mockMvc.perform(patch("/api/instances/" + instanceId + "/status")
                        .header("Authorization", "Bearer " + tenantAAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("DRAINING")));

        // Delete (decommission) instance
        mockMvc.perform(delete("/api/instances/" + instanceId)
                        .header("Authorization", "Bearer " + tenantAAdminToken))
                .andExpect(status().isOk());

        // Subsequent get returns 404 because deletedAt is set
        mockMvc.perform(get("/api/instances/" + instanceId)
                        .header("Authorization", "Bearer " + tenantAAdminToken))
                .andExpect(status().isNotFound());
    }
}
