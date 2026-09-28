package com.smartlb.authservice.service.impl;

import com.smartlb.authservice.entity.User;
import com.smartlb.authservice.exception.InvalidTokenException;
import com.smartlb.authservice.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service implementation for JWT generation, validation, and claim extraction
 * using the JJWT library with HS256 signing.
 */
@Service
public class JwtServiceImpl implements JwtService {

    // Custom claim keys
    private static final String CLAIM_ORGANIZATION_ID = "organizationId";
    private static final String CLAIM_ROLES            = "roles";
    private static final String CLAIM_EMAIL            = "email";
    private static final String CLAIM_TOKEN_TYPE       = "tokenType";
    private static final String TYPE_ACCESS            = "access";
    private static final String TYPE_REFRESH           = "refresh";

    private final String secret;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    public JwtServiceImpl(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.access-token-expiration}") long accessTokenExpirationMs,
            @Value("${security.jwt.refresh-token-expiration}") long refreshTokenExpirationMs) {
        this.secret = secret;
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    /**
     * {@inheritDoc}
     * Access token includes userId (sub), organizationId, email, roles, and tokenType=access.
     */
    @Override
    public String generateAccessToken(User user) {
        List<String> roles = user.getUserRoles().stream()
            .map(ur -> ur.getRole().getName())
            .collect(Collectors.toList());

        Map<String, Object> claims = Map.of(
            CLAIM_ORGANIZATION_ID, user.getOrganization().getId().toString(),
            CLAIM_ROLES,           roles,
            CLAIM_EMAIL,           user.getEmail(),
            CLAIM_TOKEN_TYPE,      TYPE_ACCESS
        );

        return buildToken(user.getId().toString(), claims, accessTokenExpirationMs);
    }

    /**
     * {@inheritDoc}
     * Refresh token contains only userId (sub) and tokenType=refresh; no sensitive claims.
     */
    @Override
    public String generateRefreshToken(User user) {
        Map<String, Object> claims = Map.of(
            CLAIM_TOKEN_TYPE, TYPE_REFRESH
        );
        return buildToken(user.getId().toString(), claims, refreshTokenExpirationMs);
    }

    /**
     * {@inheritDoc}
     * Returns false (does not throw) for expired, malformed, or unsigned tokens.
     */
    @Override
    public boolean validateToken(String token) {
        try {
            parseAllClaims(token);
            return true;
        } catch (ExpiredJwtException | MalformedJwtException | SignatureException
                 | UnsupportedJwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws InvalidTokenException if token is invalid or userId claim is missing
     */
    @Override
    public UUID extractUserId(String token) {
        String subject = getClaimsOrThrow(token).getSubject();
        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException e) {
            throw new InvalidTokenException("Token subject is not a valid UUID: " + subject);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws InvalidTokenException if token is invalid or organizationId claim is missing
     */
    @Override
    public UUID extractOrganizationId(String token) {
        String orgId = (String) getClaimsOrThrow(token).get(CLAIM_ORGANIZATION_ID);
        if (orgId == null) {
            throw new InvalidTokenException("Token is missing organizationId claim.");
        }
        try {
            return UUID.fromString(orgId);
        } catch (IllegalArgumentException e) {
            throw new InvalidTokenException("organizationId in token is not a valid UUID: " + orgId);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws InvalidTokenException if token is invalid or roles claim is missing
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Object roles = getClaimsOrThrow(token).get(CLAIM_ROLES);
        if (roles instanceof List<?> list) {
            return list.stream().map(Object::toString).collect(Collectors.toList());
        }
        return List.of();
    }

    /**
     * {@inheritDoc}
     *
     * @throws InvalidTokenException if token is invalid or email claim is missing
     */
    @Override
    public String extractEmail(String token) {
        String email = (String) getClaimsOrThrow(token).get(CLAIM_EMAIL);
        if (email == null) {
            throw new InvalidTokenException("Token is missing email claim.");
        }
        return email;
    }

    /**
     * {@inheritDoc}
     * Returns true if the token expiry is before the current system time.
     */
    @Override
    public boolean isTokenExpired(String token) {
        try {
            return parseAllClaims(token).getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    // ─────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────

    /**
     * Builds and signs a JWT with the provided subject, claims, and expiry duration.
     *
     * @param subject   JWT subject (typically user UUID)
     * @param claims    additional claim map to embed in the payload
     * @param expiryMs  token lifetime in milliseconds
     * @return signed JWT string
     */
    private String buildToken(String subject, Map<String, Object> claims, long expiryMs) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + expiryMs);

        return Jwts.builder()
            .setClaims(claims)
            .setSubject(subject)
            .setIssuedAt(now)
            .setExpiration(expiry)
            .signWith(signingKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    /**
     * Parses all claims from a raw JWT string.
     *
     * @param token JWT string
     * @return parsed {@link Claims} object
     */
    private Claims parseAllClaims(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(signingKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
    }

    /**
     * Parses claims and wraps any parsing exception as an {@link InvalidTokenException}.
     *
     * @param token JWT string
     * @return valid {@link Claims} object
     * @throws InvalidTokenException if parsing fails for any reason
     */
    private Claims getClaimsOrThrow(String token) {
        try {
            return parseAllClaims(token);
        } catch (ExpiredJwtException e) {
            throw new InvalidTokenException("Token has expired.");
        } catch (Exception e) {
            throw new InvalidTokenException("Token is invalid or malformed.");
        }
    }

    /**
     * Derives the HMAC-SHA signing key from the configured secret string.
     *
     * @return signing {@link Key}
     */
    private Key signingKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
