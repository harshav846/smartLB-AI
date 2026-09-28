package com.smartlb.customerservice.security.ssrf;

import com.smartlb.customerservice.exception.SsrfException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Set;

@Slf4j
@Component
public class SsrfValidator {

    private static final Set<String> BLOCKED_HOSTS = Set.of(
            "169.254.169.254",
            "metadata.google.internal",
            "instance-data"
    );

    private final boolean allowLoopback;

    public SsrfValidator(@Value("${app.security.ssrf.allow-loopback:true}") boolean allowLoopback) {
        this.allowLoopback = allowLoopback;
    }

    public void validateTarget(String host, int port) {
        if (host == null || host.isBlank()) {
            throw new SsrfException("Target host cannot be empty");
        }

        if (port < 1 || port > 65535) {
            throw new SsrfException("Target port must be between 1 and 65535");
        }

        String normalizedHost = host.trim().toLowerCase();

        if (BLOCKED_HOSTS.contains(normalizedHost)) {
            log.warn("SSRF violation blocked: Attempted access to restricted cloud metadata endpoint: {}", host);
            throw new SsrfException("Access to cloud metadata or internal management endpoints is forbidden");
        }

        try {
            InetAddress address = InetAddress.getByName(normalizedHost);

            if (address.isLinkLocalAddress()) {
                log.warn("SSRF violation blocked: Link-local address: {}", host);
                throw new SsrfException("Access to link-local addresses is forbidden");
            }

            if (address.isLoopbackAddress() && !allowLoopback) {
                log.warn("SSRF violation blocked: Loopback address disallowed: {}", host);
                throw new SsrfException("Loopback addresses are disabled for backend targets");
            }

            if (address.isMulticastAddress()) {
                log.warn("SSRF violation blocked: Multicast address: {}", host);
                throw new SsrfException("Multicast addresses are forbidden");
            }
        } catch (UnknownHostException e) {
            log.debug("Target host could not be immediately resolved via DNS: {}", host);
        }
    }
}
