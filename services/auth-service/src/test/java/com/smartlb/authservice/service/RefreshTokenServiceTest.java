package com.smartlb.authservice.service;

import com.smartlb.authservice.entity.Organization;
import com.smartlb.authservice.entity.RefreshToken;
import com.smartlb.authservice.entity.User;
import com.smartlb.authservice.exception.InvalidTokenException;
import com.smartlb.authservice.repository.RefreshTokenRepository;
import com.smartlb.authservice.repository.UserRepository;
import com.smartlb.authservice.service.impl.RefreshTokenServiceImpl;
import com.smartlb.authservice.util.TokenHashUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RefreshTokenServiceTest {

    private RefreshTokenRepository refreshTokenRepository;
    private UserRepository userRepository;
    private RefreshTokenServiceImpl refreshTokenService;

    private User testUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        userRepository = mock(UserRepository.class);
        refreshTokenService = new RefreshTokenServiceImpl(refreshTokenRepository, userRepository, 604800000L); // 7 days

        userId = UUID.randomUUID();
        Organization org = Organization.builder().id(UUID.randomUUID()).slug("test-org").name("Test Org").build();
        testUser = User.builder().id(userId).email("user@smartlb.io").organization(org).build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
    }

    @Test
    @DisplayName("Should create persistent refresh token bound to user and tenant organization")
    void testCreateRefreshToken() {
        String rawToken = refreshTokenService.createRefreshToken(userId);

        assertNotNull(rawToken);
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Should validate active refresh token successfully")
    void testValidateActiveRefreshToken() {
        String rawToken = "raw-test-token";
        String tokenHash = TokenHashUtils.hashToken(rawToken);

        RefreshToken entity = RefreshToken.builder()
                .user(testUser)
                .organization(testUser.getOrganization())
                .tokenHash(tokenHash)
                .expiryDate(OffsetDateTime.now().plusDays(1))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(entity));

        assertTrue(refreshTokenService.validateRefreshToken(rawToken));
    }

    @Test
    @DisplayName("Should reject expired or revoked refresh tokens")
    void testValidateExpiredOrRevokedRefreshToken() {
        String rawToken = "expired-token";
        String tokenHash = TokenHashUtils.hashToken(rawToken);

        RefreshToken expiredEntity = RefreshToken.builder()
                .user(testUser)
                .organization(testUser.getOrganization())
                .tokenHash(tokenHash)
                .expiryDate(OffsetDateTime.now().minusDays(1))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(expiredEntity));

        assertFalse(refreshTokenService.validateRefreshToken(rawToken));
        assertFalse(refreshTokenService.validateRefreshToken("non-existent-token"));
    }

    @Test
    @DisplayName("Should rotate active refresh token atomically")
    void testRotateRefreshToken() {
        String rawToken = "old-token";
        String tokenHash = TokenHashUtils.hashToken(rawToken);

        RefreshToken activeToken = RefreshToken.builder()
                .user(testUser)
                .organization(testUser.getOrganization())
                .tokenHash(tokenHash)
                .expiryDate(OffsetDateTime.now().plusDays(1))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(activeToken));

        String newRawToken = refreshTokenService.rotateRefreshToken(rawToken);

        assertNotNull(newRawToken);
        assertNotEquals(rawToken, newRawToken);
        assertTrue(activeToken.getRevoked());
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Should detect reuse of revoked token and revoke all user sessions")
    void testTokenReuseDetectionRevokesAllSessions() {
        String rawToken = "stolen-revoked-token";
        String tokenHash = TokenHashUtils.hashToken(rawToken);

        RefreshToken revokedToken = RefreshToken.builder()
                .user(testUser)
                .organization(testUser.getOrganization())
                .tokenHash(tokenHash)
                .expiryDate(OffsetDateTime.now().plusDays(1))
                .revoked(true)
                .build();

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(revokedToken));

        assertThrows(InvalidTokenException.class, () -> refreshTokenService.rotateRefreshToken(rawToken));
        verify(refreshTokenRepository, times(1)).revokeAllByUserId(userId);
    }
}
