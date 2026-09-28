package com.smartlb.authservice.controller;

import com.smartlb.authservice.dto.request.ChangePasswordRequest;
import com.smartlb.authservice.dto.request.ForgotPasswordRequest;
import com.smartlb.authservice.dto.request.LoginRequest;
import com.smartlb.authservice.dto.request.RefreshTokenRequest;
import com.smartlb.authservice.dto.request.RegisterOrganizationRequest;
import com.smartlb.authservice.dto.request.ResendVerificationRequest;
import com.smartlb.authservice.dto.request.ResetPasswordRequest;
import com.smartlb.authservice.dto.request.ValidateTokenRequest;
import com.smartlb.authservice.dto.request.VerifyEmailRequest;
import com.smartlb.authservice.dto.response.ApiResponse;
import com.smartlb.authservice.dto.response.LoginResponse;
import com.smartlb.authservice.dto.response.RegisterOrganizationResponse;
import com.smartlb.authservice.dto.response.TokenValidationResponse;
import com.smartlb.authservice.dto.response.UserProfileResponse;
import com.smartlb.authservice.entity.User;
import com.smartlb.authservice.service.AuthService;
import com.smartlb.authservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST controller exposing authentication, tenant onboarding, credentials recovery,
 * session management, token validation, and user profile endpoints for SmartLB-AI.
 */
@RestController
@RequestMapping({"/api/auth", "/api/v1/auth"})
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    /**
     * Onboards a new tenant Organization and creates its initial ORG_ADMIN user.
     *
     * @param request organization registration details and admin credentials
     * @return 201 Created with generated organization and admin details
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<RegisterOrganizationResponse> register(
            @Valid @RequestBody RegisterOrganizationRequest request) {
        RegisterOrganizationResponse response = authService.registerOrganization(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Authenticates user credentials and returns JWT access and refresh tokens.
     *
     * @param request user login credentials
     * @return 200 OK with tokens and user profile payload
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Rotates and issues a new access token using a valid refresh token.
     *
     * @param request refresh token request payload
     * @return 200 OK with rotated access and refresh tokens
     */
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        LoginResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Revokes active user sessions/tokens upon user logout.
     *
     * @param request optional payload containing refresh token to revoke
     * @return 200 OK confirmation message
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(@RequestBody(required = false) RefreshTokenRequest request) {
        String tokenToRevoke = (request != null) ? request.getRefreshToken() : null;
        authService.logout(tokenToRevoke);
        return ResponseEntity.ok(ApiResponse.builder()
                .message("Logged out successfully.")
                .build());
    }

    /**
     * Verifies account email using an issued verification token.
     *
     * @param request optional JSON body request containing verification token
     * @param token optional URL query parameter token
     * @return 200 OK confirmation message
     */
    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse> verifyEmail(
            @RequestBody(required = false) VerifyEmailRequest request,
            @RequestParam(name = "token", required = false) String token) {
        String tokenToVerify = (request != null && request.getToken() != null && !request.getToken().isBlank())
                ? request.getToken()
                : token;
        authService.verifyEmail(tokenToVerify);
        return ResponseEntity.ok(ApiResponse.builder()
                .message("Email verified successfully.")
                .build());
    }

    /**
     * Re-issues and dispatches account verification email.
     *
     * @param request payload containing target email
     * @return 200 OK confirmation message
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        authService.resendVerification(request);
        return ResponseEntity.ok(ApiResponse.builder()
                .message("Verification email sent successfully.")
                .build());
    }

    /**
     * Initiates password recovery process. Always returns a generic response to prevent user enumeration.
     *
     * @param request payload containing account email
     * @return 200 OK generic confirmation message
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.builder()
                .message("If the account exists, password reset instructions have been sent.")
                .build());
    }

    /**
     * Resets account password using a valid reset token.
     *
     * @param request reset token and new password
     * @return 200 OK confirmation message
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.builder()
                .message("Password reset successfully.")
                .build());
    }

    /**
     * Updates password for the authenticated user session.
     *
     * @param request current password and new password
     * @return 200 OK confirmation message
     */
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        authService.changePassword(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.builder()
                .message("Password changed successfully.")
                .build());
    }

    /**
     * Fetches current authenticated user profile.
     *
     * @return 200 OK user profile payload
     */
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser() {
        UserProfileResponse profile = authService.getCurrentUser();
        return ResponseEntity.ok(profile);
    }

    /**
     * Validates JWT token string and returns claim summary.
     *
     * @param request payload containing token string
     * @return 200 OK token validation response
     */
    @PostMapping("/validate")
    public ResponseEntity<TokenValidationResponse> validateToken(@Valid @RequestBody ValidateTokenRequest request) {
        TokenValidationResponse response = authService.validateToken(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Checks whether an email address is available for registration.
     *
     * @param email target email address
     * @return availability response boolean map
     */
    @GetMapping("/check-email")
    public ResponseEntity<Map<String, Boolean>> checkEmailAvailability(@RequestParam("email") String email) {
        boolean available = authService.checkEmailAvailability(email);
        return ResponseEntity.ok(Map.of("available", available));
    }

    /**
     * Checks whether an organization URL slug is available for registration.
     *
     * @param slug target organization slug
     * @return availability response boolean map
     */
    @GetMapping("/check-slug")
    public ResponseEntity<Map<String, Boolean>> checkSlugAvailability(@RequestParam("slug") String slug) {
        boolean available = authService.checkSlugAvailability(slug);
        return ResponseEntity.ok(Map.of("available", available));
    }
}
