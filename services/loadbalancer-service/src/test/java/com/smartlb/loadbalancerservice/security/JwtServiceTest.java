package com.smartlb.loadbalancerservice.security;

import com.smartlb.loadbalancerservice.exception.InvalidTokenException;
import com.smartlb.loadbalancerservice.service.impl.JwtServiceImpl;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String TEST_SECRET = "dGhpc2lzYWZha2VzZWNyZXRrZXlmb3J0ZXN0aW5ncHVycG9zZXNvbmx5c21hcnRsYmFpMTIzNA==";
    private JwtServiceImpl jwtService;
    private Key signingKey;

    private UUID userId;
    private UUID orgId;
    private String email;
    private List<String> roles;

    @BeforeEach
    void setUp() {
        jwtService = new JwtServiceImpl(TEST_SECRET);
        signingKey = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));

        userId = UUID.randomUUID();
        orgId = UUID.randomUUID();
        email = "admin@example.com";
        roles = List.of("ROLE_ORG_ADMIN", "ROLE_OPERATOR");
    }

    private String createToken(UUID sub, UUID org, String mail, List<String> roleList, long expirationOffsetMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationOffsetMs);

        var builder = Jwts.builder()
                .setSubject(sub != null ? sub.toString() : null)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(signingKey, SignatureAlgorithm.HS256);

        if (org != null) {
            builder.claim("organizationId", org.toString());
        }
        if (mail != null) {
            builder.claim("email", mail);
        }
        if (roleList != null) {
            builder.claim("roles", roleList);
        }

        return builder.compact();
    }

    @Test
    @DisplayName("Should successfully validate token and extract claims")
    void testValidTokenExtraction() {
        String token = createToken(userId, orgId, email, roles, 3600000);

        assertTrue(jwtService.validateToken(token));
        assertEquals(userId, jwtService.extractUserId(token));
        assertEquals(orgId, jwtService.extractOrganizationId(token));
        assertEquals(email, jwtService.extractEmail(token));
        assertEquals(roles, jwtService.extractRoles(token));
        assertFalse(jwtService.isTokenExpired(token));
    }

    @Test
    @DisplayName("Should detect expired token")
    void testExpiredToken() {
        String token = createToken(userId, orgId, email, roles, -1000);

        assertFalse(jwtService.validateToken(token));
        assertTrue(jwtService.isTokenExpired(token));
        assertThrows(InvalidTokenException.class, () -> jwtService.extractUserId(token));
    }

    @Test
    @DisplayName("Should fail validation on invalid signature")
    void testInvalidSignature() {
        Key anotherKey = Keys.hmacShaKeyFor("differentsecretkeywithminimum32characters1234567890".getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .setSubject(userId.toString())
                .claim("organizationId", orgId.toString())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(anotherKey, SignatureAlgorithm.HS256)
                .compact();

        assertFalse(jwtService.validateToken(token));
        assertThrows(InvalidTokenException.class, () -> jwtService.extractUserId(token));
    }

    @Test
    @DisplayName("Should throw when token has non-UUID subject")
    void testInvalidSubFormat() {
        Date now = new Date();
        String token = Jwts.builder()
                .setSubject("not-a-valid-uuid")
                .claim("organizationId", orgId.toString())
                .claim("email", email)
                .setExpiration(new Date(now.getTime() + 3600000))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();

        assertThrows(InvalidTokenException.class, () -> jwtService.extractUserId(token));
    }

    @Test
    @DisplayName("Should throw when organizationId is missing or invalid")
    void testMissingOrgId() {
        String tokenWithoutOrg = createToken(userId, null, email, roles, 3600000);
        assertThrows(InvalidTokenException.class, () -> jwtService.extractOrganizationId(tokenWithoutOrg));

        String tokenWithBadOrg = Jwts.builder()
                .setSubject(userId.toString())
                .claim("organizationId", "bad-uuid")
                .claim("email", email)
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
        assertThrows(InvalidTokenException.class, () -> jwtService.extractOrganizationId(tokenWithBadOrg));
    }
}
