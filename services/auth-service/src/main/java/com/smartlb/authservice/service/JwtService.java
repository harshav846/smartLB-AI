package com.smartlb.authservice.service;

import com.smartlb.authservice.entity.User;
import java.util.List;
import java.util.UUID;

/**
 * Service interface for JSON Web Token (JWT) generation, cryptographic verification,
 * and claim extraction.
 */
public interface JwtService {

    /**
     * Generates a signed JWT access token incorporating user ID, organization ID, email, and authority roles.
     *
     * @param user target User domain model
     * @return signed JWT access token string
     */
    String generateAccessToken(User user);

    /**
     * Generates a long-lived cryptographically secure refresh token string for the user.
     *
     * @param user target User domain model
     * @return signed JWT refresh token string
     */
    String generateRefreshToken(User user);

    /**
     * Cryptographically validates token signature, structure, and expiration status.
     *
     * @param token JWT token string to validate
     * @return true if valid and active, false otherwise
     */
    boolean validateToken(String token);

    /**
     * Parses subject claim to extract the unique User ID.
     *
     * @param token JWT token string
     * @return user UUID identifier
     */
    UUID extractUserId(String token);

    /**
     * Extracts tenant Organization ID claim from the token payload.
     *
     * @param token JWT token string
     * @return organization UUID identifier
     */
    UUID extractOrganizationId(String token);

    /**
     * Extracts assigned authorization role strings claim from the token payload.
     *
     * @param token JWT token string
     * @return list of role strings
     */
    List<String> extractRoles(String token);

    /**
     * Extracts user email address claim from the token payload.
     *
     * @param token JWT token string
     * @return user email address
     */
    String extractEmail(String token);

    /**
     * Checks if the token's expiration timestamp has passed.
     *
     * @param token JWT token string
     * @return true if expired, false otherwise
     */
    boolean isTokenExpired(String token);
}
