package com.smartlb.customerservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlb.customerservice.dto.request.BackendServerRequest;
import com.smartlb.customerservice.dto.request.WebsiteRequest;
import com.smartlb.customerservice.service.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CustomerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    private UUID orgA;
    private UUID orgB;
    private UUID userA;
    private UUID userB;
    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUp() {
        orgA = UUID.randomUUID();
        orgB = UUID.randomUUID();
        userA = UUID.randomUUID();
        userB = UUID.randomUUID();

        tokenA = generateJwt(userA, orgA, "alice@org-a.com");
        tokenB = generateJwt(userB, orgB, "bob@org-b.com");
    }

    private String generateJwt(UUID userId, UUID orgId, String email) {
        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("organizationId", orgId.toString())
                .claim("email", email)
                .claim("roles", List.of("TENANT_ADMIN"))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }

    @Test
    @DisplayName("Unauthenticated request to customer API should return HTTP 401")
    void unauthenticatedRequest_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/customer/websites"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("End-to-End: Tenant creates website, registers server, updates status, and Tenant B is isolated")
    void fullTenantLifecycleAndIsolation() throws Exception {
        // 1. Tenant A creates website
        WebsiteRequest websiteReq = WebsiteRequest.builder()
                .domainName("my-app-" + UUID.randomUUID().toString().substring(0, 8) + ".com")
                .displayName("My Application")
                .description("Production Service")
                .environment("PRODUCTION")
                .build();

        String createWebsiteRes = mockMvc.perform(post("/api/v1/customer/websites")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(websiteReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.domainName").value(websiteReq.getDomainName()))
                .andReturn().getResponse().getContentAsString();

        String websiteId = objectMapper.readTree(createWebsiteRes).path("data").path("id").asText();

        // 2. Tenant A registers a backend server
        BackendServerRequest serverReq = BackendServerRequest.builder()
                .serverName("backend-srv-1")
                .privateIp("192.168.1.101")
                .port(5001)
                .protocol("HTTP")
                .weight(2)
                .build();

        String createServerRes = mockMvc.perform(post("/api/v1/customer/websites/" + websiteId + "/servers")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(serverReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.serverName").value("backend-srv-1"))
                .andExpect(jsonPath("$.data.serverStatus").value("ONLINE"))
                .andReturn().getResponse().getContentAsString();

        String serverId = objectMapper.readTree(createServerRes).path("data").path("id").asText();

        // 3. Tenant A updates server status to DRAINING
        mockMvc.perform(patch("/api/v1/customer/websites/" + websiteId + "/servers/" + serverId + "/status")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "DRAINING"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.serverStatus").value("DRAINING"));

        // 4. CROSS-TENANT ISOLATION: Tenant B tries to access Tenant A's website
        mockMvc.perform(get("/api/v1/customer/websites/" + websiteId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        // 5. CROSS-TENANT ISOLATION: Tenant B tries to register server on Tenant A's website
        mockMvc.perform(post("/api/v1/customer/websites/" + websiteId + "/servers")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(serverReq)))
                .andExpect(status().isNotFound());
    }
}
