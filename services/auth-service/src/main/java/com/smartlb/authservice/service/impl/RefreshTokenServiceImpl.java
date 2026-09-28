package com.smartlb.authservice.service.impl;

import com.smartlb.authservice.entity.RefreshToken;
import com.smartlb.authservice.entity.User;
import com.smartlb.authservice.exception.InvalidTokenException;
import com.smartlb.authservice.exception.ResourceNotFoundException;
import com.smartlb.authservice.repository.RefreshTokenRepository;
import com.smartlb.authservice.repository.UserRepository;
import com.smartlb.authservice.service.RefreshTokenService;
import com.smartlb.authservice.util.TokenHashUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * Service implementation for refresh token lifecycle management backed by persistent database storage.
 *
 * <p>Enforces secure SHA-256 token hashing, single-use rotation, token revocation, tenant isolation,
 * and reuse attack detection.</p>
 */
@Service
@Transactional(readOnly = true)
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenServiceImpl.class);
    private static final int TOKEN_BYTE_LENGTH = 64;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    private final long refreshTokenExpirationMs;

    public RefreshTokenServiceImpl(
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository,
            @Value("${security.jwt.refresh-token-expiration}") long refreshTokenExpirationMs) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    /**
     * {@inheritDoc}
     * Generates a 64-byte random token, hashes it using SHA-256, and persists it with tenant context.
     */
    @Override
    @Transactional
    public String createRefreshToken(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for refresh token creation: " + userId));

        String rawToken = generateSecureToken();
        String tokenHash = TokenHashUtils.hashToken(rawToken);
        OffsetDateTime expiryDate = OffsetDateTime.now().plus(refreshTokenExpirationMs, ChronoUnit.MILLIS);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .organization(user.getOrganization())
                .tokenHash(tokenHash)
                .expiryDate(expiryDate)
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);
        log.debug("Created persistent refresh token for userId: {}", userId);
        return rawToken;
    }

    /**
     * {@inheritDoc}
     * Hashes the raw token and verifies it exists, is not revoked, and has not expired.
     */
    @Override
    public boolean validateRefreshToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        String tokenHash = TokenHashUtils.hashToken(token);
        Optional<RefreshToken> tokenOpt = refreshTokenRepository.findByTokenHash(tokenHash);

        return tokenOpt.isPresent()
                && Boolean.FALSE.equals(tokenOpt.get().getRevoked())
                && tokenOpt.get().getExpiryDate().isAfter(OffsetDateTime.now());
    }

    /**
     * {@inheritDoc}
     * Rotates refresh tokens atomically. If a revoked token is presented, detects token reuse
     * and revokes ALL sessions for that user for security protection.
     */
    @Override
    @Transactional
    public String rotateRefreshToken(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidTokenException("Refresh token is required.");
        }
        String tokenHash = TokenHashUtils.hashToken(token);
        Optional<RefreshToken> tokenOpt = refreshTokenRepository.findByTokenHash(tokenHash);

        if (tokenOpt.isEmpty()) {
            throw new InvalidTokenException("Refresh token not found.");
        }

        RefreshToken oldToken = tokenOpt.get();

        // Reuse Detection: If token was already revoked, someone may have stolen it! Revoke all user tokens.
        if (Boolean.TRUE.equals(oldToken.getRevoked())) {
            log.warn("SECURITY ALERT: Attempted reuse of revoked refresh token for userId: {}. Revoking all sessions.",
                    oldToken.getUser().getId());
            refreshTokenRepository.revokeAllByUserId(oldToken.getUser().getId());
            throw new InvalidTokenException("Refresh token has been revoked.");
        }

        if (oldToken.getExpiryDate().isBefore(OffsetDateTime.now())) {
            oldToken.setRevoked(true);
            refreshTokenRepository.save(oldToken);
            throw new InvalidTokenException("Refresh token has expired.");
        }

        // Revoke current token and issue new one
        oldToken.setRevoked(true);
        String newRawToken = createRefreshToken(oldToken.getUser().getId());
        oldToken.setReplacedByTokenHash(TokenHashUtils.hashToken(newRawToken));
        refreshTokenRepository.save(oldToken);

        return newRawToken;
    }

    /**
     * {@inheritDoc}
     * Marks a specific refresh token as revoked in persistent storage.
     */
    @Override
    @Transactional
    public void revokeRefreshToken(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        String tokenHash = TokenHashUtils.hashToken(token);
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
            log.debug("Revoked refresh token for userId: {}", rt.getUser().getId());
        });
    }

    /**
     * {@inheritDoc}
     * Revokes all active refresh tokens for the specified user ID.
     */
    @Override
    @Transactional
    public void revokeAllUserTokens(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
        log.info("Revoked all active refresh tokens for userId: {}", userId);
    }

    /**
     * Package-private helper to retrieve the owning user ID for an active refresh token.
     */
    Optional<UUID> getUserIdForToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        String tokenHash = TokenHashUtils.hashToken(token);
        return refreshTokenRepository.findByTokenHash(tokenHash)
                .filter(rt -> Boolean.FALSE.equals(rt.getRevoked()) && rt.getExpiryDate().isAfter(OffsetDateTime.now()))
                .map(rt -> rt.getUser().getId());
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
