package com.smartlb.authservice.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.smartlb.authservice.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Custom {@link UserDetails} implementation representing the security principal
 * for an authenticated SmartLB-AI user.
 *
 * <p>Preserves tenant isolation attributes ({@code userId}, {@code organizationId}) and
 * granted authority roles required for multi-tenant access control.</p>
 */
@Getter
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "userId")
public class UserPrincipal implements UserDetails {

    private final UUID userId;
    private final UUID organizationId;
    private final String email;

    @JsonIgnore
    private final String password;

    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean enabled;
    private final boolean accountNonLocked;

    /**
     * Factory method creating a {@link UserPrincipal} from a domain {@link User} entity.
     *
     * @param user domain user entity
     * @return populated UserPrincipal
     */
    public static UserPrincipal create(User user) {
        List<GrantedAuthority> authorities = user.getUserRoles().stream()
                .map(ur -> {
                    String roleName = ur.getRole().getName();
                    if (!roleName.startsWith("ROLE_")) {
                        roleName = "ROLE_" + roleName;
                    }
                    return new SimpleGrantedAuthority(roleName);
                })
                .collect(Collectors.toList());

        boolean active = "ACTIVE".equalsIgnoreCase(user.getStatus());
        boolean notLocked = !"SUSPENDED".equalsIgnoreCase(user.getStatus());

        return UserPrincipal.builder()
                .userId(user.getId())
                .organizationId(user.getOrganization() != null ? user.getOrganization().getId() : null)
                .email(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(authorities)
                .enabled(active && Boolean.TRUE.equals(user.getEmailVerified()))
                .accountNonLocked(notLocked)
                .build();
    }

    /**
     * Factory method creating a {@link UserPrincipal} directly from JWT claims.
     *
     * @param userId         unique user ID
     * @param organizationId tenant organization ID
     * @param email          user email address
     * @param roles          list of assigned role names
     * @return UserPrincipal instance for stateless request authentication
     */
    public static UserPrincipal createFromClaims(UUID userId, UUID organizationId, String email, List<String> roles) {
        List<GrantedAuthority> authorities = roles == null ? List.of() : roles.stream()
                .map(role -> {
                    String roleName = role;
                    if (!roleName.startsWith("ROLE_")) {
                        roleName = "ROLE_" + roleName;
                    }
                    return new SimpleGrantedAuthority(roleName);
                })
                .collect(Collectors.toList());

        return UserPrincipal.builder()
                .userId(userId)
                .organizationId(organizationId)
                .email(email)
                .password("")
                .authorities(authorities)
                .enabled(true)
                .accountNonLocked(true)
                .build();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
