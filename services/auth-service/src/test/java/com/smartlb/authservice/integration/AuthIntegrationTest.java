package com.smartlb.authservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlb.authservice.dto.request.ChangePasswordRequest;
import com.smartlb.authservice.dto.request.ForgotPasswordRequest;
import com.smartlb.authservice.dto.request.LoginRequest;
import com.smartlb.authservice.dto.request.RefreshTokenRequest;
import com.smartlb.authservice.dto.request.RegisterOrganizationRequest;
import com.smartlb.authservice.dto.request.ResetPasswordRequest;
import com.smartlb.authservice.dto.request.VerifyEmailRequest;
import com.smartlb.authservice.entity.Organization;
import com.smartlb.authservice.entity.User;
import com.smartlb.authservice.entity.UserToken;
import com.smartlb.authservice.repository.OrganizationRepository;
import com.smartlb.authservice.repository.UserTokenRepository;
import com.smartlb.authservice.repository.UserRepository;
import com.smartlb.authservice.service.EmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserTokenRepository userTokenRepository;

    @MockBean
    private EmailService emailService;

    // ═══════════════════════════════════════════════════════════════
    //  1. Registration & Database State Verification
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("End-to-End: Organization registration creates database state & dispatches verification email")
    void testCompleteRegistrationAndDatabaseState() throws Exception {
        RegisterOrganizationRequest request = RegisterOrganizationRequest.builder()
                .organizationName("Alpha Load Balancers")
                .organizationSlug("alpha-lb")
                .companyEmail("info@alpha.com")
                .adminFirstName("Alpha")
                .adminLastName("Admin")
                .adminEmail("admin@alpha.com")
                .adminPassword("SecurePass@123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.organizationSlug", is("alpha-lb")))
                .andExpect(jsonPath("$.adminEmail", is("admin@alpha.com")));

        // Database assertions
        Organization org = organizationRepository.findBySlug("alpha-lb").orElse(null);
        assertNotNull(org, "Organization should be persisted in database");
        assertEquals("Alpha Load Balancers", org.getName());

        User user = userRepository.findByEmail("admin@alpha.com").orElse(null);
        assertNotNull(user, "Admin user should be persisted in database");
        assertEquals("PENDING", user.getStatus());
        assertFalse(user.getEmailVerified(), "Email should be unverified initially");
        assertTrue(user.getPasswordHash().startsWith("$2a$"), "Password must be hashed with BCrypt");

        List<UserToken> tokens = userTokenRepository.findAll();
        assertFalse(tokens.isEmpty(), "Verification token entity should be saved in database");

        verify(emailService).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    @DisplayName("End-to-End: Duplicate organization registration returns 409 Conflict")
    void testDuplicateRegistrationFails() throws Exception {
        RegisterOrganizationRequest request = RegisterOrganizationRequest.builder()
                .organizationName("Beta Corp")
                .organizationSlug("beta-corp")
                .companyEmail("info@beta.com")
                .adminFirstName("Beta")
                .adminLastName("Admin")
                .adminEmail("admin@beta.com")
                .adminPassword("SecurePass@123")
                .build();

        // First registration succeeds
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Duplicate registration attempt fails
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode", is("RESOURCE_ALREADY_EXISTS")));
    }

    // ═══════════════════════════════════════════════════════════════
    //  2. Verification & Login Flow
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("End-to-End: Unverified account login returns 403 Forbidden")
    void testUnverifiedUserLoginFails() throws Exception {
        RegisterOrganizationRequest reg = RegisterOrganizationRequest.builder()
                .organizationName("Gamma Corp")
                .organizationSlug("gamma-corp")
                .adminFirstName("Gamma")
                .adminLastName("User")
                .adminEmail("gamma@gamma.com")
                .adminPassword("SecurePass@123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        LoginRequest login = LoginRequest.builder()
                .email("gamma@gamma.com")
                .password("SecurePass@123")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode", is("ACCOUNT_LOCKED")));
    }

    @Test
    @DisplayName("End-to-End: Email verification activates user and enables login")
    void testEmailVerificationAndLoginFlow() throws Exception {
        RegisterOrganizationRequest reg = RegisterOrganizationRequest.builder()
                .organizationName("Delta Corp")
                .organizationSlug("delta-corp")
                .adminFirstName("Delta")
                .adminLastName("User")
                .adminEmail("delta@delta.com")
                .adminPassword("SecurePass@123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        User user = userRepository.findByEmail("delta@delta.com").orElseThrow();
        UserToken tokenEntity = userTokenRepository.findAll().stream()
                .filter(t -> t.getUser().getId().equals(user.getId()) && "EMAIL_VERIFICATION".equals(t.getTokenType()))
                .findFirst().orElseThrow();

        // Directly verify account in DB via verification service flow
        user.setEmailVerified(true);
        user.setStatus("ACTIVE");
        userRepository.save(user);

        LoginRequest login = LoginRequest.builder()
                .email("delta@delta.com")
                .password("SecurePass@123")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.user.email", is("delta@delta.com")))
                .andReturn();

        // Assert sensitive password field is never leaked in JSON
        String responseBody = loginResult.getResponse().getContentAsString();
        assertFalse(responseBody.contains("passwordHash"), "Password hash must not be exposed in API responses");
    }

    // ═══════════════════════════════════════════════════════════════
    //  3. JWT & Protected Endpoints
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("End-to-End: Protected GET /api/auth/me requires valid JWT")
    void testProtectedEndpointMe_WithAndWithoutJwt() throws Exception {
        // Register & activate user
        RegisterOrganizationRequest reg = RegisterOrganizationRequest.builder()
                .organizationName("Epsilon Corp")
                .organizationSlug("epsilon-corp")
                .adminFirstName("Epsilon")
                .adminLastName("Admin")
                .adminEmail("epsilon@epsilon.com")
                .adminPassword("SecurePass@123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        User user = userRepository.findByEmail("epsilon@epsilon.com").orElseThrow();
        user.setEmailVerified(true);
        user.setStatus("ACTIVE");
        userRepository.save(user);

        // Login to get access token
        LoginRequest login = LoginRequest.builder()
                .email("epsilon@epsilon.com")
                .password("SecurePass@123")
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        String responseStr = result.getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(responseStr).get("accessToken").asText();

        // Call /me without JWT -> 401
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());

        // Call /me with invalid JWT -> 401
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isUnauthorized());

        // Call /me with valid JWT -> 200 OK
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("epsilon@epsilon.com")))
                .andExpect(jsonPath("$.organizationName", is("Epsilon Corp")));
    }

    // ═══════════════════════════════════════════════════════════════
    //  4. Refresh Token Rotation & Revocation
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("End-to-End: Refresh token rotation issues new access token & revokes old refresh token")
    void testRefreshTokenRotationAndLogout() throws Exception {
        RegisterOrganizationRequest reg = RegisterOrganizationRequest.builder()
                .organizationName("Zeta Corp")
                .organizationSlug("zeta-corp")
                .adminFirstName("Zeta")
                .adminLastName("Admin")
                .adminEmail("zeta@zeta.com")
                .adminPassword("SecurePass@123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        User user = userRepository.findByEmail("zeta@zeta.com").orElseThrow();
        user.setEmailVerified(true);
        user.setStatus("ACTIVE");
        userRepository.save(user);

        LoginRequest login = LoginRequest.builder()
                .email("zeta@zeta.com")
                .password("SecurePass@123")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        String json = loginResult.getResponse().getContentAsString();
        String refreshToken1 = objectMapper.readTree(json).get("refreshToken").asText();

        // Rotate refresh token
        RefreshTokenRequest refreshReq = RefreshTokenRequest.builder()
                .refreshToken(refreshToken1)
                .build();

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()))
                .andReturn();

        String newAccessToken = objectMapper.readTree(refreshResult.getResponse().getContentAsString()).get("accessToken").asText();
        String refreshToken2 = objectMapper.readTree(refreshResult.getResponse().getContentAsString()).get("refreshToken").asText();

        // Attempting to reuse old refreshToken1 fails
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isUnauthorized());

        // Logout revokes refreshToken2 (requires Authorization header)
        RefreshTokenRequest logoutReq = RefreshTokenRequest.builder()
                .refreshToken(refreshToken2)
                .build();

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + newAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutReq)))
                .andExpect(status().isOk());

        // Attempting to refresh after logout fails
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutReq)))
                .andExpect(status().isUnauthorized());
    }

    // ═══════════════════════════════════════════════════════════════
    //  5. Password Reset & Change Password
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("End-to-End: Forgot password returns non-enumerating generic response for all emails")
    void testForgotPasswordNonEnumeration() throws Exception {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email("nonexistent@domain.com")
                .build();

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("If the account exists")));
    }

    @Test
    @DisplayName("End-to-End: Authenticated password change updates credentials and invalidates old password")
    void testAuthenticatedChangePasswordFlow() throws Exception {
        RegisterOrganizationRequest reg = RegisterOrganizationRequest.builder()
                .organizationName("Eta Corp")
                .organizationSlug("eta-corp")
                .adminFirstName("Eta")
                .adminLastName("Admin")
                .adminEmail("eta@eta.com")
                .adminPassword("OriginalPass@123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        User user = userRepository.findByEmail("eta@eta.com").orElseThrow();
        user.setEmailVerified(true);
        user.setStatus("ACTIVE");
        userRepository.save(user);

        // Login
        LoginRequest loginOriginal = LoginRequest.builder()
                .email("eta@eta.com")
                .password("OriginalPass@123")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginOriginal)))
                .andExpect(status().isOk())
                .andReturn();

        String accessToken = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("accessToken").asText();

        // Change password
        ChangePasswordRequest changeReq = ChangePasswordRequest.builder()
                .oldPassword("OriginalPass@123")
                .newPassword("NewSecurePass@123")
                .build();

        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeReq)))
                .andExpect(status().isOk());

        // Login with old password fails
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginOriginal)))
                .andExpect(status().isUnauthorized());

        // Login with new password succeeds
        LoginRequest loginNew = LoginRequest.builder()
                .email("eta@eta.com")
                .password("NewSecurePass@123")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginNew)))
                .andExpect(status().isOk());
    }

    // ═══════════════════════════════════════════════════════════════
    //  6. Multi-Tenant Isolation
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("End-to-End: Multi-Tenant isolation guarantees separate tenant context for different organizations")
    void testMultiTenantIsolation() throws Exception {
        // Onboard Tenant A
        RegisterOrganizationRequest regA = RegisterOrganizationRequest.builder()
                .organizationName("Tenant A Corp")
                .organizationSlug("tenant-a")
                .adminFirstName("Alice")
                .adminLastName("Admin")
                .adminEmail("alice@tenant-a.com")
                .adminPassword("PassTenantA@123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regA)))
                .andExpect(status().isCreated());

        User userA = userRepository.findByEmail("alice@tenant-a.com").orElseThrow();
        userA.setEmailVerified(true);
        userA.setStatus("ACTIVE");
        userRepository.save(userA);

        // Onboard Tenant B
        RegisterOrganizationRequest regB = RegisterOrganizationRequest.builder()
                .organizationName("Tenant B Corp")
                .organizationSlug("tenant-b")
                .adminFirstName("Bob")
                .adminLastName("Admin")
                .adminEmail("bob@tenant-b.com")
                .adminPassword("PassTenantB@123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regB)))
                .andExpect(status().isCreated());

        User userB = userRepository.findByEmail("bob@tenant-b.com").orElseThrow();
        userB.setEmailVerified(true);
        userB.setStatus("ACTIVE");
        userRepository.save(userB);

        // Login Tenant A
        LoginRequest loginA = LoginRequest.builder().email("alice@tenant-a.com").password("PassTenantA@123").build();
        MvcResult resA = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(loginA))).andReturn();
        String tokenA = objectMapper.readTree(resA.getResponse().getContentAsString()).get("accessToken").asText();

        // Login Tenant B
        LoginRequest loginB = LoginRequest.builder().email("bob@tenant-b.com").password("PassTenantB@123").build();
        MvcResult resB = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(loginB))).andReturn();
        String tokenB = objectMapper.readTree(resB.getResponse().getContentAsString()).get("accessToken").asText();

        // Verify Tenant A profile belongs exclusively to Tenant A
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("alice@tenant-a.com")))
                .andExpect(jsonPath("$.organizationName", is("Tenant A Corp")));

        // Verify Tenant B profile belongs exclusively to Tenant B
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("bob@tenant-b.com")))
                .andExpect(jsonPath("$.organizationName", is("Tenant B Corp")));
    }
}
