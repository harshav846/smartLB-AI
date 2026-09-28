package com.smartlb.authservice.service;

import java.util.UUID;

/**
 * Service interface managing refresh token persistence, validation, token rotation,
 * and session revocation.
 */
public interface RefreshTokenService {

    /**
     * Generates and persists a new refresh token bound to a specific user.
     *
     * @param userId unique UUID key of the target user
     * @return active refresh token string
     */
    String createRefreshToken(UUID userId);

    /**
     * Validates that a refresh token exists, is active, and has not expired or been revoked.
     *
     * @param token raw refresh token string
     * @return true if valid and active, false otherwise
     */
    boolean validateRefreshToken(String token);

    /**
     * Performs atomic token rotation: revokes the provided refresh token and issues a new active token string.
     *
     * @param token active refresh token string to rotate
     * @return newly generated refresh token string
     */
    String rotateRefreshToken(String token);

    /**
     * Revokes a specific refresh token (used during single-session logout).
     *
     * @param token refresh token string to revoke
     */
    void revokeRefreshToken(String token);

    /**
     * Revokes all active refresh tokens mapped to a user (used during global logout or password reset).
     *
     * @param userId user unique UUID key
     */
    void revokeAllUserTokens(UUID userId);
}
