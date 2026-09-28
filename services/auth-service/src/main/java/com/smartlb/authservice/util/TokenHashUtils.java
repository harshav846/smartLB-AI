package com.smartlb.authservice.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Utility class providing cryptographic SHA-256 token hashing for sensitive tokens
 * (Refresh Tokens, Email Verification Tokens, Password Reset Tokens).
 *
 * <p>Ensures raw tokens are NEVER stored in plaintext in the database.</p>
 */
public final class TokenHashUtils {

    private TokenHashUtils() {
        // Utility class
    }

    /**
     * Computes a SHA-256 hex-encoded hash string for the provided raw token.
     *
     * @param rawToken raw token string
     * @return 64-character SHA-256 hexadecimal hash string
     */
    public static String hashToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("Token string cannot be null or blank for hashing");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available in current JVM", e);
        }
    }
}
