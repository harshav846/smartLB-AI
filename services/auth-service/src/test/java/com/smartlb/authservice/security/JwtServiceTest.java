package com.smartlb.authservice.security;

import com.smartlb.authservice.entity.Organization;
import com.smartlb.authservice.entity.Role;
import com.smartlb.authservice.entity.User;
import com.smartlb.authservice.entity.UserRole;
import com.smartlb.authservice.entity.UserRoleId;
import com.smartlb.authservice.service.impl.JwtServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String TEST_SECRET = "supersecretjwtkeyforunit testingandcryptographicsigning12345";
    private static final long ACCESS_EXPIRATION_MS = 3600000; // 1 hour
    private static final long REFRESH_EXPIRATION_MS = 604800000; // 7 days

    private JwtServiceImpl jwtService;
    private User testUser;
    private UUID userId;
    private UUID orgId;

    @BeforeEach
    void setUp() {
        jwtService = new JwtServiceImpl(TEST_SECRET, ACCESS_EXPIRATION_MS, REFRESH_EXPIRATION_MS);

        userId = UUID.randomUUID();
        orgId = UUID.randomUUID();

        Organization org = Organization.builder()
                .id(orgId)
                .name("SmartLB Corp")
                .slug("smartlb-corp")
                .build();

        Role role = Role.builder()
                .id(UUID.randomUUID())
                .name("ORG_ADMIN")
                .description("Organization Administrator")
                .build();

        testUser = User.builder()
                .id(userId)
                .organization(org)
                .email("admin@smartlb.io")
                .firstName("Admin")
                .lastName("User")
                .passwordHash("$2a$10$encryptedhash")
                .emailVerified(true)
                .status("ACTIVE")
                .build();

        UserRole userRole = UserRole.builder()
                .id(new UserRoleId(userId, role.getId()))
                .user(testUser)
                .role(role)
                .build();

        testUser.setUserRoles(Set.of(userRole));
    }

    @Test
    @DisplayName("Should generate valid access token containing userId, organizationId, email, and roles")
    void testGenerateAccessTokenAndExtractClaims() {
        String token = jwtService.generateAccessToken(testUser);

        assertNotNull(token);
        assertTrue(jwtService.validateToken(token));
        assertEquals(userId, jwtService.extractUserId(token));
        assertEquals(orgId, jwtService.extractOrganizationId(token));
        assertEquals("admin@smartlb.io", jwtService.extractEmail(token));

        List<String> roles = jwtService.extractRoles(token);
        assertNotNull(roles);
        assertEquals(1, roles.size());
        assertEquals("ORG_ADMIN", roles.get(0));
    }

    @Test
    @DisplayName("Should reject invalid or malformed tokens during validation")
    void testValidateInvalidToken() {
        assertFalse(jwtService.validateToken("invalid.jwt.token"));
        assertFalse(jwtService.validateToken(""));
        assertFalse(jwtService.validateToken(null));
    }

    @Test
    @DisplayName("Should reject token signed with a different secret")
    void testValidateTokenWithForgedSecret() {
        JwtServiceImpl otherKeyService = new JwtServiceImpl(
                "differentsecretkeyforforgedtokensigning1234567890",
                ACCESS_EXPIRATION_MS,
                REFRESH_EXPIRATION_MS
        );

        String forgedToken = otherKeyService.generateAccessToken(testUser);
        assertFalse(jwtService.validateToken(forgedToken));
    }

    @Test
    @DisplayName("Should reject expired token")
    void testExpiredToken() {
        JwtServiceImpl shortLivedService = new JwtServiceImpl(TEST_SECRET, -1000L, REFRESH_EXPIRATION_MS);

        String expiredToken = shortLivedService.generateAccessToken(testUser);
        assertFalse(jwtService.validateToken(expiredToken));
        assertTrue(jwtService.isTokenExpired(expiredToken));
    }
}
