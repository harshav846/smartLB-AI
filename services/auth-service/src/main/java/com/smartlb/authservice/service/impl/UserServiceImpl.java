package com.smartlb.authservice.service.impl;

import com.smartlb.authservice.dto.request.RegisterOrganizationRequest;
import com.smartlb.authservice.dto.response.UserProfileResponse;
import com.smartlb.authservice.entity.Organization;
import com.smartlb.authservice.entity.Role;
import com.smartlb.authservice.entity.User;
import com.smartlb.authservice.entity.UserRole;
import com.smartlb.authservice.entity.UserRoleId;
import com.smartlb.authservice.exception.DuplicateResourceException;
import com.smartlb.authservice.exception.UserNotFoundException;
import com.smartlb.authservice.mapper.UserMapper;
import com.smartlb.authservice.repository.UserRepository;
import com.smartlb.authservice.service.RoleService;
import com.smartlb.authservice.repository.UserRoleRepository;
import com.smartlb.authservice.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Service implementation for User entity lifecycle, credential management,
 * account locking, and security context resolution.
 */
@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    /** Maximum consecutive failed login attempts before account is locked. */
    private static final int MAX_FAILED_ATTEMPTS = 5;

    private static final String ORG_ADMIN_ROLE = "ORG_ADMIN";

    private final UserRepository userRepository;
    private final RoleService roleService;
    private final UserRoleRepository userRoleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository,
                           RoleService roleService,
                           UserRoleRepository userRoleRepository,
                           UserMapper userMapper,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleService    = roleService;
        this.userRoleRepository = userRoleRepository;
        this.userMapper     = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * {@inheritDoc}
     * Creates the ORG_ADMIN user for the newly registered organization.
     * Password is BCrypt-hashed; account status is set to PENDING until email is verified.
     *
     * @throws DuplicateResourceException if the admin email is already registered
     */
    @Override
    @Transactional
    public User createAdminUser(Organization organization, RegisterOrganizationRequest request) {
        if (userRepository.existsByEmail(request.getAdminEmail())) {
            throw new DuplicateResourceException(
                "Email '" + request.getAdminEmail() + "' is already registered.");
        }

        User user = User.builder()
            .organization(organization)
            .firstName(request.getAdminFirstName())
            .lastName(request.getAdminLastName())
            .email(request.getAdminEmail())
            .passwordHash(passwordEncoder.encode(request.getAdminPassword()))
            .phone(request.getPhone())
            .emailVerified(false)
            .failedLoginAttempts(0)
            .status("PENDING")
            .build();

        User savedUser = userRepository.save(user);
        assignRoleToUser(savedUser, ORG_ADMIN_ROLE);
        return savedUser;
    }

    /**
     * {@inheritDoc}
     *
     * @throws UserNotFoundException if no user exists with the given email
     */
    @Override
    public UserProfileResponse findByEmail(String email) {
        return userRepository.findByEmailAndDeletedAtIsNull(email)
            .map(userMapper::toResponse)
            .orElseThrow(() -> new UserNotFoundException(
                "User not found with email: " + email));
    }

    /**
     * {@inheritDoc}
     *
     * @throws UserNotFoundException if no user exists with the given ID
     */
    @Override
    public UserProfileResponse findById(UUID id) {
        return userRepository.findByIdAndDeletedAtIsNull(id)
            .map(userMapper::toResponse)
            .orElseThrow(() -> new UserNotFoundException(
                "User not found with ID: " + id));
    }

    /**
     * {@inheritDoc}
     *
     * @throws UserNotFoundException if no user exists with the given email
     */
    @Override
    public User getEntityByEmail(String email) {
        return userRepository.findByEmailAndDeletedAtIsNull(email)
            .orElseThrow(() -> new UserNotFoundException(
                "User not found with email: " + email));
    }

    /**
     * {@inheritDoc}
     *
     * @throws UserNotFoundException if no user exists with the given ID
     */
    @Override
    public User getEntityById(UUID id) {
        return userRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new UserNotFoundException(
                "User not found with ID: " + id));
    }

    /**
     * {@inheritDoc}
     * Updates password hash and sets lastPasswordChange to the current timestamp.
     *
     * @throws UserNotFoundException if no user exists with the given ID
     */
    @Override
    @Transactional
    public void updatePassword(UUID userId, String newEncodedPassword) {
        User user = getEntityById(userId);
        user.setPasswordHash(newEncodedPassword);
        user.setLastPasswordChange(OffsetDateTime.now());
        userRepository.save(user);
    }

    /**
     * {@inheritDoc}
     * Sets emailVerified to true and transitions status from PENDING to ACTIVE.
     *
     * @throws UserNotFoundException if no user exists with the given ID
     */
    @Override
    @Transactional
    public void verifyEmail(UUID userId) {
        User user = getEntityById(userId);
        user.setEmailVerified(true);
        if ("PENDING".equals(user.getStatus())) {
            user.setStatus("ACTIVE");
        }
        userRepository.save(user);
    }

    /**
     * {@inheritDoc}
     * Increments the counter; locks the account if MAX_FAILED_ATTEMPTS is reached.
     *
     * @throws UserNotFoundException if no user exists with the given ID
     */
    @Override
    @Transactional
    public void incrementFailedLoginAttempts(UUID userId) {
        User user = getEntityById(userId);
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setStatus("SUSPENDED");
        }
        userRepository.save(user);
    }

    /**
     * {@inheritDoc}
     *
     * @throws UserNotFoundException if no user exists with the given ID
     */
    @Override
    @Transactional
    public void resetFailedLoginAttempts(UUID userId) {
        User user = getEntityById(userId);
        user.setFailedLoginAttempts(0);
        userRepository.save(user);
    }

    /**
     * {@inheritDoc}
     * Sets user status to SUSPENDED to prevent future logins.
     *
     * @throws UserNotFoundException if no user exists with the given ID
     */
    @Override
    @Transactional
    public void lockAccount(UUID userId) {
        User user = getEntityById(userId);
        user.setStatus("SUSPENDED");
        userRepository.save(user);
    }

    /**
     * {@inheritDoc}
     * Resolves the authenticated user entity from the Spring Security context.
     *
     * @throws UsernameNotFoundException if no user matches the principal email in the security context
     */
    @Override
    public User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmailAndDeletedAtIsNull(email)
            .orElseThrow(() -> new UsernameNotFoundException(
                "Authenticated user not found: " + email));
    }

    /** {@inheritDoc} */
    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    // ─────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────

    /**
     * Resolves or creates a Role by name via {@link RoleService} and assigns it to the given user.
     *
     * <p>Role resolution is delegated entirely to the {@link RoleService} service layer
     * rather than directly accessing {@link com.smartlb.authservice.repository.RoleRepository}.
     * This preserves cross-domain service boundaries and ensures all role business logic
     * remains centralized in {@link com.smartlb.authservice.service.impl.RoleServiceImpl}.</p>
     *
     * @param user     target user entity
     * @param roleName name of the role to assign (e.g. {@code ORG_ADMIN})
     */
    private void assignRoleToUser(User user, String roleName) {
        Role role = roleService.findOrCreate(roleName, "Organization Administrator role");

        UserRoleId userRoleId = new UserRoleId(user.getId(), role.getId());
        UserRole userRole = UserRole.builder()
            .id(userRoleId)
            .user(user)
            .role(role)
            .build();
        userRoleRepository.save(userRole);
    }
}
