package com.smartlb.apigateway.filter;

import com.smartlb.apigateway.security.JwtTokenService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GatewaySecurityAndRoutingTest {

    private static final String SECRET = "dGhpc2lzYWZha2VzZWNyZXRrZXlmb3J0ZXN0aW5ncHVycG9zZXNvbmx5c21hcnRsYmFpMTIzNA==";

    private JwtTokenService jwtTokenService;
    private JwtAuthenticationFilter jwtFilter;
    private CorrelationIdFilter correlationIdFilter;
    private RateLimiterFilter rateLimiterFilter;
    private GatewayFilterChain filterChain;

    @BeforeEach
    void setUp() {
        jwtTokenService = new JwtTokenService(SECRET);
        jwtFilter = new JwtAuthenticationFilter(jwtTokenService);
        correlationIdFilter = new CorrelationIdFilter();
        rateLimiterFilter = new RateLimiterFilter(true, 5, 2); // 2 burst capacity
        filterChain = mock(GatewayFilterChain.class);
        when(filterChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());
    }

    private String generateToken(UUID userId, UUID orgId, long expirationDeltaMs) {
        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("organizationId", orgId.toString())
                .claim("email", "user@test.com")
                .claim("roles", List.of("TENANT_ADMIN"))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationDeltaMs))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }

    @Test
    @DisplayName("CorrelationIdFilter should inject X-Correlation-Id header when absent")
    void correlationIdFilter_injectsHeader() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/customer/websites").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        correlationIdFilter.filter(exchange, filterChain).block();

        assertThat(exchange.getResponse().getHeaders().getFirst("X-Correlation-Id")).isNotNull();
    }

    @Test
    @DisplayName("JwtAuthenticationFilter should allow public paths without Authorization header")
    void jwtFilter_publicPath_allowsAccess() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/auth/login").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        jwtFilter.filter(exchange, filterChain).block();

        assertThat(exchange.getResponse().getStatusCode()).isNull(); // Not rejected
    }

    @Test
    @DisplayName("JwtAuthenticationFilter should reject protected paths without token with HTTP 401")
    void jwtFilter_missingToken_rejectsWith401() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/customer/websites").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        jwtFilter.filter(exchange, filterChain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("JwtAuthenticationFilter should forward claims as headers for valid token")
    void jwtFilter_validToken_forwardHeaders() {
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        String token = generateToken(userId, orgId, 3600000);

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/customer/websites")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(filterChain.filter(any(ServerWebExchange.class))).thenAnswer(inv -> {
            ServerWebExchange mutated = inv.getArgument(0);
            assertThat(mutated.getRequest().getHeaders().getFirst("X-User-Id")).isEqualTo(userId.toString());
            assertThat(mutated.getRequest().getHeaders().getFirst("X-Organization-Id")).isEqualTo(orgId.toString());
            return Mono.empty();
        });

        jwtFilter.filter(exchange, filterChain).block();
    }

    @Test
    @DisplayName("RateLimiterFilter should return HTTP 429 when burst capacity is exceeded")
    void rateLimiterFilter_burstExceeded_returns429() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/customer/websites")
                .remoteAddress(new java.net.InetSocketAddress("127.0.0.1", 12345))
                .build();

        // 1st request -> allowed (capacity: 2 -> 1)
        MockServerWebExchange ex1 = MockServerWebExchange.from(request);
        rateLimiterFilter.filter(ex1, filterChain).block();
        assertThat(ex1.getResponse().getStatusCode()).isNull();

        // 2nd request -> allowed (capacity: 1 -> 0)
        MockServerWebExchange ex2 = MockServerWebExchange.from(request);
        rateLimiterFilter.filter(ex2, filterChain).block();
        assertThat(ex2.getResponse().getStatusCode()).isNull();

        // 3rd request -> blocked (429 Too Many Requests)
        MockServerWebExchange ex3 = MockServerWebExchange.from(request);
        rateLimiterFilter.filter(ex3, filterChain).block();
        assertThat(ex3.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }
}
