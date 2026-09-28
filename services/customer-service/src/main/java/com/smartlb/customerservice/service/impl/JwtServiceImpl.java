package com.smartlb.customerservice.service.impl;

import com.smartlb.customerservice.exception.InvalidTokenException;
import com.smartlb.customerservice.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class JwtServiceImpl implements JwtService {

    private static final String CLAIM_ORGANIZATION_ID = "organizationId";
    private static final String CLAIM_ROLES            = "roles";
    private static final String CLAIM_EMAIL            = "email";

    private final String secret;

    public JwtServiceImpl(@Value("${security.jwt.secret}") String secret) {
        this.secret = secret;
    }

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

    @Override
    public UUID extractUserId(String token) {
        String subject = getClaimsOrThrow(token).getSubject();
        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException e) {
            throw new InvalidTokenException("Token subject is not a valid UUID: " + subject);
        }
    }

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

    @Override
    public String extractEmail(String token) {
        String email = (String) getClaimsOrThrow(token).get(CLAIM_EMAIL);
        if (email == null) {
            throw new InvalidTokenException("Token is missing email claim.");
        }
        return email;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Object roles = getClaimsOrThrow(token).get(CLAIM_ROLES);
        if (roles instanceof List<?> list) {
            return list.stream().map(Object::toString).collect(Collectors.toList());
        }
        return List.of();
    }

    @Override
    public boolean isTokenExpired(String token) {
        try {
            return parseAllClaims(token).getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    private Claims parseAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Claims getClaimsOrThrow(String token) {
        try {
            return parseAllClaims(token);
        } catch (ExpiredJwtException e) {
            throw new InvalidTokenException("JWT token has expired");
        } catch (MalformedJwtException | SignatureException | UnsupportedJwtException | IllegalArgumentException e) {
            throw new InvalidTokenException("Invalid JWT token: " + e.getMessage());
        }
    }

    private Key getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
