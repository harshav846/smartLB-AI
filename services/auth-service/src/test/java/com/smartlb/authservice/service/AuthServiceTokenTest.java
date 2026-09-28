package com.smartlb.authservice.service;

import com.smartlb.authservice.dto.request.ChangePasswordRequest;
import com.smartlb.authservice.dto.request.ForgotPasswordRequest;
import com.smartlb.authservice.dto.request.ResetPasswordRequest;
import com.smartlb.authservice.entity.Organization;
import com.smartlb.authservice.entity.User;
import com.smartlb.authservice.entity.UserToken;
import com.smartlb.authservice.exception.BadCredentialsException;
import com.smartlb.authservice.exception.InvalidTokenException;
import com.smartlb.authservice.mapper.UserMapper;
import com.smartlb.authservice.repository.UserTokenRepository;
import com.smartlb.authservice.service.impl.AuthServiceImpl;
import com.smartlb.authservice.util.TokenHashUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTokenTest {

    private OrganizationService organizationService;
    private UserService userService;
    private JwtService jwtService;
    private RefreshTokenService refreshTokenService;
    private EmailService emailService;
    private UserMapper userMapper;
    private PasswordEncoder passwordEncoder;
    private UserTokenRepository userTokenRepository;

    private AuthServiceImpl authService;

    private User testUser;
    private UUID userId;
    private String currentPasswordHash;

    @BeforeEach
    void setUp() {
        organizationService = mock(OrganizationService.class);
        userService = mock(UserService.class);
        jwtService = mock(JwtService.class);
        refreshTokenService = mock(RefreshTokenService.class);
        emailService = mock(EmailService.class);
        userMapper = mock(UserMapper.class);
        passwordEncoder = new BCryptPasswordEncoder();
        userTokenRepository = mock(UserTokenRepository.class);

        authService = new AuthServiceImpl(
                organizationService,
                userService,
                jwtService,
                refreshTokenService,
                emailService,
                userMapper,
                passwordEncoder,
                userTokenRepository
        );

        userId = UUID.randomUUID();
        currentPasswordHash = passwordEncoder.encode("CurrentPass123!");

        Organization org = Organization.builder().id(UUID.randomUUID()).slug("acme").name("Acme Corp").build();
        testUser = User.builder()
                .id(userId)
                .email("user@acme.com")
                .passwordHash(currentPasswordHash)
                .organization(org)
                .emailVerified(true)
                .status("ACTIVE")
                .build();

        when(userService.getEntityById(userId)).thenReturn(testUser);
        when(userService.getEntityByEmail("user@acme.com")).thenReturn(testUser);
    }

    @Test
    @DisplayName("Should process forgot password and send reset email without user enumeration")
    void testForgotPasswordSuccess() {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder().email("user@acme.com").build();

        authService.forgotPassword(request);

        verify(userTokenRepository, times(1)).invalidateAllByUserIdAndTokenType(userId, "PASSWORD_RESET");
        verify(userTokenRepository, times(1)).save(any(UserToken.class));
        verify(emailService, times(1)).sendPasswordResetEmail(eq("user@acme.com"), anyString());
    }

    @Test
    @DisplayName("Should swallow exceptions for non-existent email in forgotPassword to prevent user enumeration")
    void testForgotPasswordNonExistentUserSilent() {
        when(userService.getEntityByEmail("unknown@acme.com")).thenThrow(new RuntimeException("User not found"));

        ForgotPasswordRequest request = ForgotPasswordRequest.builder().email("unknown@acme.com").build();

        assertDoesNotThrow(() -> authService.forgotPassword(request));
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    @DisplayName("Should reset password with valid token, encode password via BCrypt, and revoke sessions")
    void testResetPasswordSuccess() {
        String rawToken = "valid-reset-token";
        String tokenHash = TokenHashUtils.hashToken(rawToken);

        UserToken resetTokenEntity = UserToken.builder()
                .user(testUser)
                .organization(testUser.getOrganization())
                .tokenHash(tokenHash)
                .tokenType("PASSWORD_RESET")
                .expiryDate(OffsetDateTime.now().plusHours(1))
                .used(false)
                .build();

        when(userTokenRepository.findByTokenHashAndTokenTypeAndUsedFalse(tokenHash, "PASSWORD_RESET"))
                .thenReturn(Optional.of(resetTokenEntity));

        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token(rawToken)
                .newPassword("NewBrandPass2026!")
                .build();

        authService.resetPassword(request);

        verify(userService, times(1)).updatePassword(eq(userId), argThat(pass -> passwordEncoder.matches("NewBrandPass2026!", pass)));
        verify(refreshTokenService, times(1)).revokeAllUserTokens(userId);
        assertTrue(resetTokenEntity.getUsed());
    }

    @Test
    @DisplayName("Should reject password reset when new password is identical to current password")
    void testResetPasswordSamePasswordRejected() {
        String rawToken = "valid-reset-token";
        String tokenHash = TokenHashUtils.hashToken(rawToken);

        UserToken resetTokenEntity = UserToken.builder()
                .user(testUser)
                .organization(testUser.getOrganization())
                .tokenHash(tokenHash)
                .tokenType("PASSWORD_RESET")
                .expiryDate(OffsetDateTime.now().plusHours(1))
                .used(false)
                .build();

        when(userTokenRepository.findByTokenHashAndTokenTypeAndUsedFalse(tokenHash, "PASSWORD_RESET"))
                .thenReturn(Optional.of(resetTokenEntity));

        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token(rawToken)
                .newPassword("CurrentPass123!") // Same password
                .build();

        assertThrows(BadCredentialsException.class, () -> authService.resetPassword(request));
    }

    @Test
    @DisplayName("Should verify email with valid token and mark single-use token as used")
    void testVerifyEmailSuccess() {
        String rawToken = "valid-verify-token";
        String tokenHash = TokenHashUtils.hashToken(rawToken);

        UserToken verifyTokenEntity = UserToken.builder()
                .user(testUser)
                .organization(testUser.getOrganization())
                .tokenHash(tokenHash)
                .tokenType("EMAIL_VERIFICATION")
                .expiryDate(OffsetDateTime.now().plusHours(24))
                .used(false)
                .build();

        when(userTokenRepository.findByTokenHashAndTokenTypeAndUsedFalse(tokenHash, "EMAIL_VERIFICATION"))
                .thenReturn(Optional.of(verifyTokenEntity));

        authService.verifyEmail(rawToken);

        verify(userService, times(1)).verifyEmail(userId);
        assertTrue(verifyTokenEntity.getUsed());
    }

    @Test
    @DisplayName("Should reject expired email verification token")
    void testVerifyEmailExpiredTokenRejected() {
        String rawToken = "expired-verify-token";
        String tokenHash = TokenHashUtils.hashToken(rawToken);

        UserToken verifyTokenEntity = UserToken.builder()
                .user(testUser)
                .organization(testUser.getOrganization())
                .tokenHash(tokenHash)
                .tokenType("EMAIL_VERIFICATION")
                .expiryDate(OffsetDateTime.now().minusHours(1)) // Expired
                .used(false)
                .build();

        when(userTokenRepository.findByTokenHashAndTokenTypeAndUsedFalse(tokenHash, "EMAIL_VERIFICATION"))
                .thenReturn(Optional.of(verifyTokenEntity));

        assertThrows(InvalidTokenException.class, () -> authService.verifyEmail(rawToken));
    }

    @Test
    @DisplayName("Should change password for authenticated user and revoke active refresh sessions")
    void testChangePasswordSuccess() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .oldPassword("CurrentPass123!")
                .newPassword("SuperSecretNewPass2026!")
                .build();

        authService.changePassword(userId, request);

        verify(userService, times(1)).updatePassword(eq(userId), argThat(pass -> passwordEncoder.matches("SuperSecretNewPass2026!", pass)));
        verify(refreshTokenService, times(1)).revokeAllUserTokens(userId);
    }

    @Test
    @DisplayName("Should reject change password when current password does not match")
    void testChangePasswordIncorrectOldPasswordRejected() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .oldPassword("WrongPassword123!")
                .newPassword("SuperSecretNewPass2026!")
                .build();

        assertThrows(BadCredentialsException.class, () -> authService.changePassword(userId, request));
    }
}
