package com.smartlb.authservice.security;

import com.smartlb.authservice.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    private JwtService jwtService;
    private JwtAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        jwtService = mock(JwtService.class);
        filter = new JwtAuthenticationFilter(jwtService);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should populate SecurityContext when valid Bearer JWT is provided in request header")
    void testFilterPopulatesSecurityContextForValidJwt() throws ServletException, IOException {
        String token = "valid.jwt.token";
        request.addHeader("Authorization", "Bearer " + token);

        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();

        when(jwtService.validateToken(token)).thenReturn(true);
        when(jwtService.extractUserId(token)).thenReturn(userId);
        when(jwtService.extractOrganizationId(token)).thenReturn(orgId);
        when(jwtService.extractEmail(token)).thenReturn("user@smartlb.io");
        when(jwtService.extractRoles(token)).thenReturn(List.of("ORG_ADMIN"));

        filter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.isAuthenticated());

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        assertEquals(userId, principal.getUserId());
        assertEquals(orgId, principal.getOrganizationId());
        assertEquals("user@smartlb.io", principal.getEmail());
        assertTrue(principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ORG_ADMIN")));

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should leave SecurityContext unauthenticated when Authorization header is missing")
    void testFilterIgnoresMissingAuthorizationHeader() throws ServletException, IOException {
        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should leave SecurityContext unauthenticated when invalid Bearer token is provided")
    void testFilterIgnoresInvalidJwt() throws ServletException, IOException {
        String token = "invalid.jwt.token";
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtService.validateToken(token)).thenReturn(false);

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }
}
