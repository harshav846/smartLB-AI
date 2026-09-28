package com.smartlb.authservice.service.impl;

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
import com.smartlb.authservice.entity.Organization;
import com.smartlb.authservice.entity.User;
import com.smartlb.authservice.entity.UserToken;
import com.smartlb.authservice.exception.AccountStatusException;
import com.smartlb.authservice.exception.BadCredentialsException;
import com.smartlb.authservice.exception.AuthException;
import com.smartlb.authservice.exception.EmailNotVerifiedException;
import com.smartlb.authservice.exception.InvalidTokenException;
import com.smartlb.authservice.mapper.UserMapper;
import com.smartlb.authservice.repository.UserTokenRepository;
import com.smartlb.authservice.service.AuthService;
import com.smartlb.authservice.service.EmailService;
import com.smartlb.authservice.service.JwtService;
import com.smartlb.authservice.service.OrganizationService;
import com.smartlb.authservice.service.RefreshTokenService;
import com.smartlb.authservice.service.UserService;
import com.smartlb.authservice.util.TokenHashUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * Top-level facade service orchestrating all authentication, registration, and session
 * management flows for the SmartLB-AI platform.
 *
 * <p>Enforces database-backed single-use SHA-256 tokens for email verification and password reset,
 * BCrypt password hashing, multi-tenant isolation, and session revocation.</p>
 */
@Service
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private static final int    OTP_BYTE_LENGTH      = 32;
    private static final long   RESET_TOKEN_TTL_MS   = 3_600_000L;    // 1 hour
    private static final long   VERIFY_TOKEN_TTL_MS  = 86_400_000L;   // 24 hours
    private static final long   ACCESS_TOKEN_EXPIRY_SECONDS = 3600L;   // 1 hour

    private static final String TYPE_VERIFICATION = "EMAIL_VERIFICATION";
    private static final String TYPE_RESET        = "PASSWORD_RESET";

    private final OrganizationService organizationService;
    private final UserService         userService;
    private final JwtService          jwtService;
    private final RefreshTokenService refreshTokenService;
    private final EmailService        emailService;
    private final UserMapper          userMapper;
    private final PasswordEncoder     passwordEncoder;
    private final UserTokenRepository userTokenRepository;
    private final SecureRandom        secureRandom;

    public AuthServiceImpl(OrganizationService organizationService,
                           UserService userService,
                           JwtService jwtService,
                           RefreshTokenService refreshTokenService,
                           EmailService emailService,
                           UserMapper userMapper,
                           PasswordEncoder passwordEncoder,
                           UserTokenRepository userTokenRepository) {
        this.organizationService = organizationService;
        this.userService         = userService;
        this.jwtService          = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.emailService        = emailService;
        this.userMapper          = userMapper;
        this.passwordEncoder     = passwordEncoder;
        this.userTokenRepository = userTokenRepository;
        this.secureRandom        = new SecureRandom();
    }

    // ═══════════════════════════════════════════════════════════════
    //  Registration
    // ═══════════════════════════════════════════════════════════════

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public RegisterOrganizationResponse registerOrganization(RegisterOrganizationRequest request) {
        log.info("Registering new organization: {}", request.getOrganizationSlug());

        Organization organization = organizationService.createOrganization(request);
        User adminUser = userService.createAdminUser(organization, request);

        String verificationToken = generateOtp();
        saveUserToken(adminUser, organization, verificationToken, TYPE_VERIFICATION, VERIFY_TOKEN_TTL_MS);

        emailService.sendVerificationEmail(adminUser.getEmail(), verificationToken);

        log.info("Organization '{}' registered. Verification email dispatched to {}.",
            organization.getSlug(), adminUser.getEmail());

        return RegisterOrganizationResponse.builder()
            .organizationId(organization.getId())
            .organizationSlug(organization.getSlug())
            .adminId(adminUser.getId())
            .adminEmail(adminUser.getEmail())
            .message("Registration successful. Please verify your email to activate the account.")
            .build();
    }

    // ═══════════════════════════════════════════════════════════════
    //  Authentication
    // ═══════════════════════════════════════════════════════════════

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user;
        try {
            user = userService.getEntityByEmail(request.getEmail());
        } catch (Exception e) {
            throw new BadCredentialsException("Invalid email or password.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            userService.incrementFailedLoginAttempts(user.getId());
            throw new BadCredentialsException("Invalid email or password.");
        }

        checkAccountStatus(user);

        if (!user.getEmailVerified()) {
            throw new EmailNotVerifiedException(
                "Please verify your email address before logging in.");
        }

        userService.resetFailedLoginAttempts(user.getId());

        String accessToken  = jwtService.generateAccessToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(user.getId());

        return buildLoginResponse(user, accessToken, refreshToken);
    }

    // ═══════════════════════════════════════════════════════════════
    //  Token Refresh (Rotation)
    // ═══════════════════════════════════════════════════════════════

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        String oldRefreshToken = request.getRefreshToken();

        if (!refreshTokenService.validateRefreshToken(oldRefreshToken)) {
            throw new InvalidTokenException("Refresh token is invalid or has expired.");
        }

        UUID userId = getUserIdFromRefreshToken(oldRefreshToken);
        String newRefreshToken = refreshTokenService.rotateRefreshToken(oldRefreshToken);

        User user = userService.getEntityById(userId);
        checkAccountStatus(user);

        String newAccessToken = jwtService.generateAccessToken(user);

        return buildLoginResponse(user, newAccessToken, newRefreshToken);
    }

    // ═══════════════════════════════════════════════════════════════
    //  Session Management
    // ═══════════════════════════════════════════════════════════════

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void logout(String refreshToken) {
        if (refreshTokenService.validateRefreshToken(refreshToken)) {
            UUID userId = getUserIdFromRefreshToken(refreshToken);
            refreshTokenService.revokeAllUserTokens(userId);
        } else {
            refreshTokenService.revokeRefreshToken(refreshToken);
        }
        log.debug("Logout processed for refresh token.");
    }

    // ═══════════════════════════════════════════════════════════════
    //  Password Management
    // ═══════════════════════════════════════════════════════════════

    /**
     * {@inheritDoc}
     * Always returns a generic response to prevent user enumeration attacks.
     */
    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        try {
            User user = userService.getEntityByEmail(request.getEmail());
            if ("ACTIVE".equals(user.getStatus()) && Boolean.TRUE.equals(user.getEmailVerified())) {
                userTokenRepository.invalidateAllByUserIdAndTokenType(user.getId(), TYPE_RESET);

                String resetToken = generateOtp();
                saveUserToken(user, user.getOrganization(), resetToken, TYPE_RESET, RESET_TOKEN_TTL_MS);

                emailService.sendPasswordResetEmail(user.getEmail(), resetToken);
                log.info("Password reset email dispatched to {}", user.getEmail());
            }
        } catch (Exception e) {
            // Swallow all exceptions to prevent user enumeration
            log.debug("forgotPassword: account not found or ineligible for reset: {}", request.getEmail());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        UserToken tokenEntity = findValidUserToken(request.getToken(), TYPE_RESET,
            "Password reset token is invalid or has expired.");

        User user = tokenEntity.getUser();

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException(
                "New password must be different from the current password.");
        }

        String encoded = passwordEncoder.encode(request.getNewPassword());
        userService.updatePassword(user.getId(), encoded);
        userService.resetFailedLoginAttempts(user.getId());

        refreshTokenService.revokeAllUserTokens(user.getId());

        tokenEntity.setUsed(true);
        userTokenRepository.save(tokenEntity);

        log.info("Password reset completed for userId: {}", user.getId());
    }

    // ═══════════════════════════════════════════════════════════════
    //  Email Verification
    // ═══════════════════════════════════════════════════════════════

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void verifyEmail(String token) {
        UserToken tokenEntity = findValidUserToken(token, TYPE_VERIFICATION,
            "Email verification token is invalid or has expired.");

        userService.verifyEmail(tokenEntity.getUser().getId());

        tokenEntity.setUsed(true);
        userTokenRepository.save(tokenEntity);

        log.info("Email verified for userId: {}", tokenEntity.getUser().getId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void resendVerification(ResendVerificationRequest request) {
        User user = userService.getEntityByEmail(request.getEmail());

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            return;
        }

        userTokenRepository.invalidateAllByUserIdAndTokenType(user.getId(), TYPE_VERIFICATION);

        String newToken = generateOtp();
        saveUserToken(user, user.getOrganization(), newToken, TYPE_VERIFICATION, VERIFY_TOKEN_TTL_MS);

        emailService.resendVerificationEmail(user.getEmail(), newToken);
        log.info("Verification email re-sent to {}", user.getEmail());
    }

    // ═══════════════════════════════════════════════════════════════
    //  Authenticated Operations
    // ═══════════════════════════════════════════════════════════════

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userService.getEntityById(userId);

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Current password is incorrect.");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new AuthException("New password must be different from the current password.");
        }

        String encoded = passwordEncoder.encode(request.getNewPassword());
        userService.updatePassword(userId, encoded);
        refreshTokenService.revokeAllUserTokens(userId);
        log.info("Password changed and all sessions revoked for userId: {}", userId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserProfileResponse getCurrentUser() {
        User user = userService.getCurrentAuthenticatedUser();
        return userMapper.toResponse(user);
    }

    // ═══════════════════════════════════════════════════════════════
    //  Token Validation & Availability Checks
    // ═══════════════════════════════════════════════════════════════

    /**
     * {@inheritDoc}
     */
    @Override
    public TokenValidationResponse validateToken(ValidateTokenRequest request) {
        String token = request.getToken();

        if (!jwtService.validateToken(token)) {
            return TokenValidationResponse.builder()
                .valid(false)
                .build();
        }

        UUID userId         = jwtService.extractUserId(token);
        UUID organizationId = jwtService.extractOrganizationId(token);
        List<String> roles  = jwtService.extractRoles(token);

        return TokenValidationResponse.builder()
            .valid(true)
            .userId(userId)
            .organizationId(organizationId)
            .roles(roles)
            .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean checkEmailAvailability(String email) {
        return !userService.existsByEmail(email);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean checkSlugAvailability(String slug) {
        return !organizationService.existsBySlug(slug);
    }

    // ═══════════════════════════════════════════════════════════════
    //  Private helpers
    // ═══════════════════════════════════════════════════════════════

    private void checkAccountStatus(User user) {
        switch (user.getStatus()) {
            case "SUSPENDED" -> throw new AccountStatusException(
                "Your account has been suspended. Please contact support.");
            case "PENDING" -> throw new AccountStatusException(
                "Your account is not yet active. Please verify your email address.");
            case "DELETED" -> throw new AccountStatusException(
                "This account no longer exists.");
            default -> {}
        }
    }

    private LoginResponse buildLoginResponse(User user, String accessToken, String refreshToken) {
        return LoginResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .expiresIn(ACCESS_TOKEN_EXPIRY_SECONDS)
            .user(userMapper.toResponse(user))
            .build();
    }

    private String generateOtp() {
        byte[] bytes = new byte[OTP_BYTE_LENGTH];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void saveUserToken(User user, Organization org, String rawToken, String type, long ttlMs) {
        String tokenHash = TokenHashUtils.hashToken(rawToken);
        UserToken token = UserToken.builder()
                .user(user)
                .organization(org)
                .tokenHash(tokenHash)
                .tokenType(type)
                .expiryDate(OffsetDateTime.now().plus(ttlMs, ChronoUnit.MILLIS))
                .used(false)
                .build();
        userTokenRepository.save(token);
    }

    private UserToken findValidUserToken(String rawToken, String type, String errorMessage) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidTokenException(errorMessage);
        }
        String tokenHash = TokenHashUtils.hashToken(rawToken);
        UserToken token = userTokenRepository.findByTokenHashAndTokenTypeAndUsedFalse(tokenHash, type)
                .orElseThrow(() -> new InvalidTokenException(errorMessage));

        if (token.getExpiryDate().isBefore(OffsetDateTime.now())) {
            token.setUsed(true);
            userTokenRepository.save(token);
            throw new InvalidTokenException(errorMessage);
        }
        return token;
    }

    private UUID getUserIdFromRefreshToken(String refreshToken) {
        if (refreshTokenService instanceof RefreshTokenServiceImpl impl) {
            return impl.getUserIdForToken(refreshToken)
                .orElseThrow(() -> new InvalidTokenException(
                    "Refresh token not found or expired."));
        }
        throw new InvalidTokenException("Cannot resolve user from refresh token.");
    }
}
