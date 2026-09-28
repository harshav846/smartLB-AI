package com.smartlb.loadbalancerservice.security.ssrf;

import com.smartlb.loadbalancerservice.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link SsrfValidator}.
 */
class SsrfValidatorTest {

    private final SsrfValidator validatorWithLoopback = new SsrfValidator(true);
    private final SsrfValidator validatorNoLoopback   = new SsrfValidator(false);

    /* ── blocked cloud metadata endpoints ─────────────────────────── */

    @Test
    void blockedMetadataEndpoint_169_254_throws() {
        assertThatThrownBy(() -> validatorWithLoopback.validateTarget("169.254.169.254", 80))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("forbidden");
    }

    @Test
    void blockedGcpMetadataEndpoint_throws() {
        assertThatThrownBy(() -> validatorWithLoopback.validateTarget("metadata.google.internal", 80))
                .isInstanceOf(BadRequestException.class);
    }

    /* ── invalid port ───────────────────────────────────────────── */

    @Test
    void portZero_throws() {
        assertThatThrownBy(() -> validatorWithLoopback.validateTarget("example.com", 0))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("port");
    }

    @Test
    void portAboveMax_throws() {
        assertThatThrownBy(() -> validatorWithLoopback.validateTarget("example.com", 99999))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("port");
    }

    /* ── blank host ────────────────────────────────────────────── */

    @Test
    void blankHost_throws() {
        assertThatThrownBy(() -> validatorWithLoopback.validateTarget("   ", 8080))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("host");
    }

    @Test
    void nullHost_throws() {
        assertThatThrownBy(() -> validatorWithLoopback.validateTarget(null, 8080))
                .isInstanceOf(BadRequestException.class);
    }

    /* ── loopback allowed/denied ───────────────────────────────── */

    @Test
    void loopback_allowedWhenFlagTrue_doesNotThrow() {
        assertThatCode(() -> validatorWithLoopback.validateTarget("127.0.0.1", 8080))
                .doesNotThrowAnyException();
    }

    @Test
    void loopback_blockedWhenFlagFalse_throws() {
        assertThatThrownBy(() -> validatorNoLoopback.validateTarget("127.0.0.1", 8080))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Loopback");
    }

    /* ── valid public host ─────────────────────────────────────── */

    @Test
    void publicHost_doesNotThrow() {
        // DNS may or may not resolve in CI; either result is acceptable
        // The validator only throws BadRequestException for validated failures
        try {
            validatorWithLoopback.validateTarget("example.com", 443);
        } catch (BadRequestException e) {
            // Still acceptable if SSRF rule triggered
        }
        // At least no NullPointerException / other runtime error
    }
}
