package com.smartlb.authservice.repository;

import com.smartlb.authservice.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Role entity operations.
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    /**
     * Finds a role matching the name parameter.
     * @param name authorization privilege string (e.g. ORG_ADMIN)
     * @return an Optional enclosing the role if present
     */
    Optional<Role> findByName(String name);

    /**
     * Checks if a role with the name exists.
     * @param name authorization privilege string
     * @return true if exists, false otherwise
     */
    boolean existsByName(String name);
}
