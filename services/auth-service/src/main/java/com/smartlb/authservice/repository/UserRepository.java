package com.smartlb.authservice.repository;

import com.smartlb.authservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for User entity operations.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Finds a user by their email address.
     * @param email the user email query
     * @return an Optional enclosing the user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks if a user registration exists matching the specified email address.
     * @param email user email query
     * @return true if registered, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Finds all users mapping under an organization context.
     * @param organizationId parent organization UUID identifier
     * @return list of matching User models
     */
    List<User> findByOrganizationId(UUID organizationId);

    /**
     * Locates an active user by their email ensuring it has not been soft-deleted.
     * @param email user email query
     * @return an Optional enclosing the user if active
     */
    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    /**
     * Locates an active user by their ID ensuring it has not been soft-deleted.
     * @param id user unique UUID key
     * @return an Optional enclosing the user if active
     */
    Optional<User> findByIdAndDeletedAtIsNull(UUID id);
}
