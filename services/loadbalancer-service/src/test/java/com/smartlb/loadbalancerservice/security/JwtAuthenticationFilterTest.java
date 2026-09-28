package com.smartlb.loadbalancerservice.security;

import com.smartlb.loadbalancerservice.service.JwtService;
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
    @DisplayName("Should populate SecurityContext when valid Bearer JWT is provided")
    void testValidBearerToken() throws ServletException, IOException {
        String token = "valid.jwt.token";
        request.addHeader("Authorization", "Bearer " + token);

        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        when(jwtService.validateToken(token)).thenReturn(true);
        when(jwtService.extractUserId(token)).thenReturn(userId);
        when(jwtService.extractOrganizationId(token)).thenReturn(orgId);
        when(jwtService.extractEmail(token)).thenReturn("operator@smartlb.io");
        when(jwtService.extractRoles(token)).thenReturn(List.of("ROLE_OPERATOR"));

        filter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertTrue(auth.isAuthenticated());

        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        assertEquals(userId, principal.getUserId());
        assertEquals(orgId, principal.getOrganizationId());
        assertEquals("operator@smartlb.io", principal.getEmail());
        assertTrue(principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_OPERATOR")));

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Should not authenticate when header is missing or non-Bearer")
    void testMissingHeader() throws ServletException, IOException {
        filter.doFilter(request, response, filterChain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");
        filter.doFilter(request, response, filterChain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        verify(filterChain, times(2)).doFilter(request, response);
    }

    @Test
    @DisplayName("Should not authenticate when JWT is invalid")
    void testInvalidToken() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer invalid.token");
        when(jwtService.validateToken("invalid.token")).thenReturn(false);

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, times(1)).doFilter(request, response);
    }
}
