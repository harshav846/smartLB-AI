package com.smartlb.authservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlb.authservice.dto.request.ChangePasswordRequest;
import com.smartlb.authservice.dto.request.ForgotPasswordRequest;
import com.smartlb.authservice.dto.request.LoginRequest;
import com.smartlb.authservice.dto.request.RefreshTokenRequest;
import com.smartlb.authservice.dto.request.RegisterOrganizationRequest;
import com.smartlb.authservice.dto.request.ResendVerificationRequest;
import com.smartlb.authservice.dto.request.ResetPasswordRequest;
import com.smartlb.authservice.dto.request.ValidateTokenRequest;
import com.smartlb.authservice.dto.request.VerifyEmailRequest;
import com.smartlb.authservice.dto.response.LoginResponse;
import com.smartlb.authservice.dto.response.RegisterOrganizationResponse;
import com.smartlb.authservice.dto.response.TokenValidationResponse;
import com.smartlb.authservice.dto.response.UserProfileResponse;
import com.smartlb.authservice.entity.User;
import com.smartlb.authservice.exception.BadCredentialsException;
import com.smartlb.authservice.exception.DuplicateResourceException;
import com.smartlb.authservice.exception.InvalidTokenException;
import com.smartlb.authservice.security.JwtAuthenticationEntryPoint;
import com.smartlb.authservice.security.JwtAccessDeniedHandler;
import com.smartlb.authservice.service.JwtService;
import com.smartlb.authservice.service.AuthService;
import com.smartlb.authservice.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockBean
    private JwtAccessDeniedHandler jwtAccessDeniedHandler;

    private UUID userId;
    private UUID orgId;
    private User mockUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        orgId = UUID.randomUUID();

        mockUser = User.builder()
                .id(userId)
                .email("admin@acme.com")
                .firstName("Admin")
                .lastName("User")
                .status("ACTIVE")
                .emailVerified(true)
                .build();
    }

    @Test
    @DisplayName("POST /api/auth/register - Success returns 201 Created")
    void testRegisterOrganization_Success() throws Exception {
        RegisterOrganizationRequest request = RegisterOrganizationRequest.builder()
                .organizationName("Acme Corp")
                .organizationSlug("acme-corp")
                .companyEmail("info@acme.com")
                .adminFirstName("Jane")
                .adminLastName("Doe")
                .adminEmail("jane@acme.com")
                .adminPassword("Password@123")
                .build();

        RegisterOrganizationResponse response = RegisterOrganizationResponse.builder()
                .organizationId(orgId)
                .organizationSlug("acme-corp")
                .adminId(userId)
                .adminEmail("jane@acme.com")
                .message("Registration successful.")
                .build();

        given(authService.registerOrganization(any(RegisterOrganizationRequest.class))).willReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.organizationSlug", is("acme-corp")))
                .andExpect(jsonPath("$.adminEmail", is("jane@acme.com")));
    }

    @Test
    @DisplayName("POST /api/auth/register - Validation error returns 400 Bad Request")
    void testRegisterOrganization_ValidationError() throws Exception {
        RegisterOrganizationRequest request = RegisterOrganizationRequest.builder()
                .organizationName("")
                .organizationSlug("invalid slug!")
                .adminEmail("not-an-email")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("POST /api/auth/register - Duplicate organization returns 409 Conflict")
    void testRegisterOrganization_Duplicate() throws Exception {
        RegisterOrganizationRequest request = RegisterOrganizationRequest.builder()
                .organizationName("Acme Corp")
                .organizationSlug("acme-corp")
                .companyEmail("info@acme.com")
                .adminFirstName("Jane")
                .adminLastName("Doe")
                .adminEmail("jane@acme.com")
                .adminPassword("Password@123")
                .build();

        given(authService.registerOrganization(any(RegisterOrganizationRequest.class)))
                .willThrow(new DuplicateResourceException("Organization slug already exists."));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/auth/login - Success returns 200 OK")
    void testLogin_Success() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("admin@acme.com")
                .password("Password@123")
                .build();

        LoginResponse response = LoginResponse.builder()
                .accessToken("mock-access-token")
                .refreshToken("mock-refresh-token")
                .expiresIn(3600L)
                .build();

        given(authService.login(any(LoginRequest.class))).willReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", is("mock-access-token")))
                .andExpect(jsonPath("$.refreshToken", is("mock-refresh-token")));
    }

    @Test
    @DisplayName("POST /api/auth/login - Bad Credentials returns 401 Unauthorized")
    void testLogin_BadCredentials() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("admin@acme.com")
                .password("WrongPassword")
                .build();

        given(authService.login(any(LoginRequest.class)))
                .willThrow(new BadCredentialsException("Invalid email or password."));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/auth/refresh - Success returns 200 OK")
    void testRefreshToken_Success() throws Exception {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("valid-refresh-token")
                .build();

        LoginResponse response = LoginResponse.builder()
                .accessToken("new-access-token")
                .refreshToken("rotated-refresh-token")
                .expiresIn(3600L)
                .build();

        given(authService.refreshToken(any(RefreshTokenRequest.class))).willReturn(response);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", is("new-access-token")));
    }

    @Test
    @DisplayName("POST /api/auth/refresh - Invalid refresh token returns 401 Unauthorized")
    void testRefreshToken_Invalid() throws Exception {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("invalid-refresh-token")
                .build();

        given(authService.refreshToken(any(RefreshTokenRequest.class)))
                .willThrow(new InvalidTokenException("Refresh token is invalid or has expired."));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/auth/logout - Success returns 200 OK")
    void testLogout_Success() throws Exception {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("token-to-revoke")
                .build();

        doNothing().when(authService).logout("token-to-revoke");

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Logged out successfully.")));

        verify(authService).logout("token-to-revoke");
    }

    @Test
    @DisplayName("POST /api/auth/verify-email - Success returns 200 OK")
    void testVerifyEmail_Success() throws Exception {
        VerifyEmailRequest request = VerifyEmailRequest.builder()
                .token("valid-verify-token")
                .build();

        doNothing().when(authService).verifyEmail("valid-verify-token");

        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Email verified successfully.")));
    }

    @Test
    @DisplayName("POST /api/auth/resend-verification - Success returns 200 OK")
    void testResendVerification_Success() throws Exception {
        ResendVerificationRequest request = ResendVerificationRequest.builder()
                .email("admin@acme.com")
                .build();

        doNothing().when(authService).resendVerification(any(ResendVerificationRequest.class));

        mockMvc.perform(post("/api/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Verification email sent successfully.")));
    }

    @Test
    @DisplayName("POST /api/auth/forgot-password - Success returns 200 OK with non-enumerating message")
    void testForgotPassword_Success() throws Exception {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email("user@acme.com")
                .build();

        doNothing().when(authService).forgotPassword(any(ForgotPasswordRequest.class));

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("If the account exists, password reset instructions have been sent.")));
    }

    @Test
    @DisplayName("POST /api/auth/reset-password - Success returns 200 OK")
    void testResetPassword_Success() throws Exception {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("valid-reset-token")
                .newPassword("NewPassword@123")
                .build();

        doNothing().when(authService).resetPassword(any(ResetPasswordRequest.class));

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Password reset successfully.")));
    }

    @Test
    @DisplayName("POST /api/auth/change-password - Success returns 200 OK")
    void testChangePassword_Success() throws Exception {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .oldPassword("OldPassword@123")
                .newPassword("NewPassword@123")
                .build();

        given(userService.getCurrentAuthenticatedUser()).willReturn(mockUser);
        doNothing().when(authService).changePassword(eq(userId), any(ChangePasswordRequest.class));

        mockMvc.perform(post("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Password changed successfully.")));

        verify(authService).changePassword(eq(userId), any(ChangePasswordRequest.class));
    }

    @Test
    @DisplayName("GET /api/auth/me - Success returns user profile")
    void testGetCurrentUser_Success() throws Exception {
        UserProfileResponse profile = UserProfileResponse.builder()
                .userId(userId)
                .email("admin@acme.com")
                .firstName("Admin")
                .lastName("User")
                .organizationId(orgId)
                .organizationName("Acme Corp")
                .roles(List.of("ROLE_ORG_ADMIN"))
                .build();

        given(authService.getCurrentUser()).willReturn(profile);

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("admin@acme.com")))
                .andExpect(jsonPath("$.organizationName", is("Acme Corp")));
    }

    @Test
    @DisplayName("POST /api/auth/validate - Success returns token claims")
    void testValidateToken_Success() throws Exception {
        ValidateTokenRequest request = ValidateTokenRequest.builder()
                .token("sample-jwt-token")
                .build();

        TokenValidationResponse response = TokenValidationResponse.builder()
                .valid(true)
                .userId(userId)
                .organizationId(orgId)
                .roles(List.of("ROLE_ORG_ADMIN"))
                .build();

        given(authService.validateToken(any(ValidateTokenRequest.class))).willReturn(response);

        mockMvc.perform(post("/api/auth/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(true)))
                .andExpect(jsonPath("$.userId", is(userId.toString())));
    }

    @Test
    @DisplayName("GET /api/auth/check-email - Returns email availability status")
    void testCheckEmail_Availability() throws Exception {
        given(authService.checkEmailAvailability("new@acme.com")).willReturn(true);

        mockMvc.perform(get("/api/auth/check-email").param("email", "new@acme.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available", is(true)));
    }

    @Test
    @DisplayName("GET /api/auth/check-slug - Returns slug availability status")
    void testCheckSlug_Availability() throws Exception {
        given(authService.checkSlugAvailability("new-slug")).willReturn(true);

        mockMvc.perform(get("/api/auth/check-slug").param("slug", "new-slug"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available", is(true)));
    }
}
