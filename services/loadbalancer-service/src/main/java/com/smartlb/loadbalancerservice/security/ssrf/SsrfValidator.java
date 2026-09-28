package com.smartlb.loadbalancerservice.security.ssrf;

import com.smartlb.loadbalancerservice.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Set;

/**
 * Validates backend host targets to protect against Server-Side Request Forgery (SSRF).
 * Blocks access to cloud metadata services, link-local addresses, and sensitive internal ranges.
 */
@Slf4j
@Component
public class SsrfValidator {

    private static final Set<String> BLOCKED_HOSTS = Set.of(
            "169.254.169.254", // AWS/GCP/Azure instance metadata
            "metadata.google.internal",
            "instance-data"
    );

    private final boolean allowLoopback;

    public SsrfValidator(@Value("${app.security.ssrf.allow-loopback:true}") boolean allowLoopback) {
        this.allowLoopback = allowLoopback;
    }

    public void validateTarget(String host, int port) {
        if (host == null || host.isBlank()) {
            throw new BadRequestException("Target host cannot be empty");
        }

        if (port < 1 || port > 65535) {
            throw new BadRequestException("Target port must be between 1 and 65535");
        }

        String normalizedHost = host.trim().toLowerCase();

        if (BLOCKED_HOSTS.contains(normalizedHost)) {
            log.warn("SSRF violation blocked: Attempted access to restricted cloud metadata endpoint: {}", host);
            throw new BadRequestException("Access to cloud metadata or internal management endpoints is forbidden");
        }

        try {
            InetAddress address = InetAddress.getByName(normalizedHost);

            if (address.isLinkLocalAddress() || address.isSiteLocalAddress() && !allowLoopback) {
                log.warn("SSRF violation blocked: Link-local or forbidden site-local address: {}", host);
                throw new BadRequestException("Access to link-local addresses is forbidden");
            }

            if (address.isLoopbackAddress() && !allowLoopback) {
                log.warn("SSRF violation blocked: Loopback address disallowed in production: {}", host);
                throw new BadRequestException("Loopback addresses are disabled for backend targets");
            }

            if (address.isMulticastAddress()) {
                log.warn("SSRF violation blocked: Multicast address: {}", host);
                throw new BadRequestException("Multicast addresses are forbidden");
            }
        } catch (UnknownHostException e) {
            // Unresolvable host will be caught at connect time, but log debug
            log.debug("Target host could not be immediately resolved via DNS: {}", host);
        }
    }
}
