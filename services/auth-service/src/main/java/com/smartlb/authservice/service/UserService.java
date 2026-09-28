package com.smartlb.authservice.service;

import com.smartlb.authservice.dto.request.RegisterOrganizationRequest;
import com.smartlb.authservice.dto.response.UserProfileResponse;
import com.smartlb.authservice.entity.Organization;
import com.smartlb.authservice.entity.User;
import java.util.UUID;

/**
 * Service interface for User domain operations, credentials lifecycle, account locking,
 * failed attempt counters, and security context resolution.
 */
public interface UserService {

    /**
     * Creates and persists the primary administrator user for a new organization tenant.
     *
     * @param organization parent Organization entity
     * @param request payload containing administrator user details and initial credentials
     * @return persisted User entity
     */
    User createAdminUser(Organization organization, RegisterOrganizationRequest request);

    /**
     * Locates user profile details matching the given email address.
     *
     * @param email email address query
     * @return UserProfileResponse DTO
     */
    UserProfileResponse findByEmail(String email);

    /**
     * Locates user profile details matching the given unique user identifier.
     *
     * @param id user unique UUID identifier
     * @return UserProfileResponse DTO
     */
    UserProfileResponse findById(UUID id);

    /**
     * Internal domain method retrieving raw User entity by email.
     *
     * @param email email address query
     * @return User domain entity
     */
    User getEntityByEmail(String email);

    /**
     * Internal domain method retrieving raw User entity by unique identifier.
     *
     * @param id user unique UUID identifier
     * @return User domain entity
     */
    User getEntityById(UUID id);

    /**
     * Updates the password hash and sets lastPasswordChange timestamp for a user.
     *
     * @param userId user unique UUID key
     * @param newEncodedPassword BCrypt encoded password string
     */
    void updatePassword(UUID userId, String newEncodedPassword);

    /**
     * Flags a user email address as verified.
     *
     * @param userId user unique UUID key
     */
    void verifyEmail(UUID userId);

    /**
     * Increments failed login attempt counter for a user and locks account if threshold exceeded.
     *
     * @param userId user unique UUID key
     */
    void incrementFailedLoginAttempts(UUID userId);

    /**
     * Resets failed login attempt counter to zero upon successful authentication.
     *
     * @param userId user unique UUID key
     */
    void resetFailedLoginAttempts(UUID userId);

    /**
     * Locks/suspends a user account due to security violations or administrative lock.
     *
     * @param userId user unique UUID key
     */
    void lockAccount(UUID userId);

    /**
     * Resolves the current authenticated User domain entity from Spring Security SecurityContext.
     *
     * @return active User entity
     */
    User getCurrentAuthenticatedUser();

    /**
     * Checks if a user registration exists matching the specified email.
     *
     * @param email email address query
     * @return true if registered, false otherwise
     */
    boolean existsByEmail(String email);
}
