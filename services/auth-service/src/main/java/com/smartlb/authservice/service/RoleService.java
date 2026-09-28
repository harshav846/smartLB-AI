package com.smartlb.authservice.service;

import com.smartlb.authservice.entity.Role;

/**
 * Service interface for Role entity lookup and lifecycle management.
 *
 * <p>Centralises all role resolution logic so that other services (e.g. UserService)
 * consume role operations through the service tier rather than directly through
 * the repository tier, preserving proper domain layering.</p>
 */
public interface RoleService {

    /**
     * Retrieves the Role entity matching the given name.
     *
     * @param name authorization role name (e.g. {@code ORG_ADMIN}, {@code USER})
     * @return Role domain entity
     * @throws com.smartlb.authservice.exception.ResourceNotFoundException if no role exists with the given name
     */
    Role findByName(String name);

    /**
     * Checks whether a role with the specified name exists in the system.
     *
     * @param name authorization role name string
     * @return {@code true} if a matching role exists, {@code false} otherwise
     */
    boolean existsByName(String name);

    /**
     * Atomically resolves an existing role by name or creates and persists a new one.
     *
     * <p>This find-or-create pattern guarantees idempotency during tenant onboarding:
     * the ORG_ADMIN role will be located if already seeded, or created fresh if the
     * system is bootstrapping for the first time.</p>
     *
     * @param name        authorization role name (e.g. {@code ORG_ADMIN})
     * @param description human-readable description of the role's purpose
     * @return the existing or newly-persisted {@link Role} entity
     */
    Role findOrCreate(String name, String description);
}
