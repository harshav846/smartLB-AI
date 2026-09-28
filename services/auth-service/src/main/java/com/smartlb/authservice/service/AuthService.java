package com.smartlb.authservice.service;

import com.smartlb.authservice.dto.request.ChangePasswordRequest;
import com.smartlb.authservice.dto.request.ForgotPasswordRequest;
import com.smartlb.authservice.dto.request.LoginRequest;
import com.smartlb.authservice.dto.request.RefreshTokenRequest;
import com.smartlb.authservice.dto.request.RegisterOrganizationRequest;
import com.smartlb.authservice.dto.request.ResendVerificationRequest;
import com.smartlb.authservice.dto.request.ResetPasswordRequest;
import com.smartlb.authservice.dto.request.ValidateTokenRequest;
import com.smartlb.authservice.dto.response.LoginResponse;
import com.smartlb.authservice.dto.response.RegisterOrganizationResponse;
import com.smartlb.authservice.dto.response.TokenValidationResponse;
import com.smartlb.authservice.dto.response.UserProfileResponse;
import java.util.UUID;

/**
 * High-level application service interface orchestrating authentication flows, tenant onboarding,
 * credentials management, session control, and token verification.
 */
public interface AuthService {

    /**
     * Onboards a new corporate Organization tenant and its initial administrator user.
     *
     * @param request payload containing tenant organization details and administrator credentials
     * @return response summary detailing generated organization and admin identifiers
     */
    RegisterOrganizationResponse registerOrganization(RegisterOrganizationRequest request);

    /**
     * Authenticates user credentials and issues active JWT access and refresh tokens.
     *
     * @param request payload containing user login credentials (email and password)
     * @return response payload containing JWT access token, refresh token, expiry, and user profile
     */
    LoginResponse login(LoginRequest request);

    /**
     * Refreshes expired access token credentials using a valid, active refresh token.
     *
     * @param request payload enclosing the active refresh token
     * @return response containing a new access token and rotated refresh token
     */
    LoginResponse refreshToken(RefreshTokenRequest request);

    /**
     * Invalidates a user session by revoking the provided refresh token.
     *
     * @param refreshToken raw refresh token string to revoke
     */
    void logout(String refreshToken);

    /**
     * Initiates the password reset workflow by dispatching a recovery token to the user's email.
     *
     * @param request payload containing the target account email address
     */
    void forgotPassword(ForgotPasswordRequest request);

    /**
     * Resets a user password using a verified recovery token.
     *
     * @param request payload containing the reset token and the new password
     */
    void resetPassword(ResetPasswordRequest request);

    /**
     * Verifies user email address using an email verification token.
     *
     * @param token verification token received by user via email
     */
    void verifyEmail(String token);

    /**
     * Re-issues and dispatches an email verification token to the specified user.
     *
     * @param request payload containing target user email address
     */
    void resendVerification(ResendVerificationRequest request);

    /**
     * Changes password for an actively authenticated user session.
     *
     * @param userId unique identifier of the user
     * @param request payload containing current password and new desired password
     */
    void changePassword(UUID userId, ChangePasswordRequest request);

    /**
     * Retrieves the profile metadata of the currently authenticated user context.
     *
     * @return user profile information of current security context user
     */
    UserProfileResponse getCurrentUser();

    /**
     * Validates a JWT token string and returns claim details and tenant context.
     *
     * @param request payload containing token to be validated
     * @return summary response containing token validity, user ID, organization ID, and roles
     */
    TokenValidationResponse validateToken(ValidateTokenRequest request);

    /**
     * Checks whether an email address is available for registration.
     *
     * @param email email address string to check
     * @return true if available, false if already registered
     */
    boolean checkEmailAvailability(String email);

    /**
     * Checks whether an organization URL slug is available for registration.
     *
     * @param slug organization slug string to check
     * @return true if available, false if already taken
     */
    boolean checkSlugAvailability(String slug);
}
