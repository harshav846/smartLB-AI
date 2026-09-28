package com.smartlb.authservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Core application infrastructure bean configuration.
 *
 * <p>Provides foundational beans required by the service layer that are not
 * domain-specific but are infrastructure dependencies (e.g. password encoding).</p>
 *
 * <p><strong>Security boundary note:</strong> Defining the {@link PasswordEncoder} bean
 * here (rather than inside a security configuration class) keeps it accessible to the
 * service layer without forcing premature coupling to a not-yet-implemented Spring
 * Security configuration. The full {@code SecurityFilterChain} and JWT filter will
 * be added in the upcoming Security phase.</p>
 */
@Configuration
public class AppConfig {

    /**
     * Provides a BCrypt password encoder for one-way password hashing in the service layer.
     *
     * <p>BCrypt with the default strength (10 rounds) is the industry standard for
     * password storage. It is intentionally slow to resist brute-force attacks.
     * The same bean is consumed by:</p>
     * <ul>
     *   <li>{@link com.smartlb.authservice.service.impl.UserServiceImpl} — for hashing
     *       the admin password during organization registration and for updating passwords.</li>
     *   <li>{@link com.smartlb.authservice.service.impl.AuthServiceImpl} — for verifying
     *       credentials during login and password-change operations.</li>
     * </ul>
     *
     * @return BCryptPasswordEncoder instance (strength = 10)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
