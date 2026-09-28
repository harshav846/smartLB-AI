package com.smartlb.apigateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Component
public class RateLimiterFilter implements GlobalFilter, Ordered {

    private final boolean enabled;
    private final int replenishRate;
    private final int burstCapacity;

    private final ConcurrentMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public RateLimiterFilter(
            @Value("${gateway.rate-limiting.enabled:true}") boolean enabled,
            @Value("${gateway.rate-limiting.replenish-rate:50}") int replenishRate,
            @Value("${gateway.rate-limiting.burst-capacity:100}") int burstCapacity) {
        this.enabled = enabled;
        this.replenishRate = replenishRate;
        this.burstCapacity = burstCapacity;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!enabled) {
            return chain.filter(exchange);
        }

        String clientKey = resolveClientKey(exchange);
        TokenBucket bucket = buckets.computeIfAbsent(clientKey, k -> new TokenBucket(burstCapacity, replenishRate));

        if (!bucket.tryConsume(1)) {
            log.warn("Rate limit exceeded for client: {} on path: {}", clientKey, exchange.getRequest().getPath());
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            response.getHeaders().add("Retry-After", "1");

            String body = "{\"success\":false,\"message\":\"Rate limit exceeded. Please slow down.\",\"timestamp\":\"" + java.time.Instant.now() + "\"}";
            DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        }

        return chain.filter(exchange);
    }

    private String resolveClientKey(ServerWebExchange exchange) {
        // Prefer X-Forwarded-For if behind a proxy, else remote address
        String forwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            return remoteAddress.getAddress().getHostAddress();
        }
        return "anonymous";
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 5;
    }

    private static class TokenBucket {
        private final double capacity;
        private final double replenishRatePerSecond;
        private double tokens;
        private long lastRefillTimestampNanos;

        public TokenBucket(double capacity, double replenishRatePerSecond) {
            this.capacity = capacity;
            this.replenishRatePerSecond = replenishRatePerSecond;
            this.tokens = capacity;
            this.lastRefillTimestampNanos = System.nanoTime();
        }

        public synchronized boolean tryConsume(double tokensRequested) {
            refill();
            if (tokens >= tokensRequested) {
                tokens -= tokensRequested;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.nanoTime();
            double secondsElapsed = (now - lastRefillTimestampNanos) / 1_000_000_000.0;
            tokens = Math.min(capacity, tokens + secondsElapsed * replenishRatePerSecond);
            lastRefillTimestampNanos = now;
        }
    }
}
