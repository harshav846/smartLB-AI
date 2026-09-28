package com.smartlb.authservice.service.impl;

import com.smartlb.authservice.entity.Role;
import com.smartlb.authservice.exception.ResourceNotFoundException;
import com.smartlb.authservice.repository.RoleRepository;
import com.smartlb.authservice.service.RoleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for Role entity lookup and lifecycle management.
 *
 * <p>Wraps {@link RoleRepository} behind the service tier to ensure that no
 * other service or component directly accesses the role persistence layer.
 * This preserves domain boundaries and allows role business rules to evolve
 * independently of the callers.</p>
 */
@Service
@Transactional(readOnly = true)
public class RoleServiceImpl implements RoleService {

    private static final Logger log = LoggerFactory.getLogger(RoleServiceImpl.class);

    private final RoleRepository roleRepository;

    public RoleServiceImpl(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    /**
     * {@inheritDoc}
     *
     * @throws ResourceNotFoundException if no role is found with the given name
     */
    @Override
    public Role findByName(String name) {
        return roleRepository.findByName(name)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Role not found with name: " + name));
    }

    /** {@inheritDoc} */
    @Override
    public boolean existsByName(String name) {
        return roleRepository.existsByName(name);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Uses an optimistic find-or-create pattern backed by the repository layer.
     * If the role already exists (e.g. pre-seeded in the database), it is returned
     * directly. If not, a new {@link Role} entity is constructed and persisted
     * within the current transaction.</p>
     *
     * <p><strong>Multi-tenant note:</strong> Roles are global system roles, not
     * per-tenant. A role such as {@code ORG_ADMIN} is shared across all organizations;
     * tenant-specific authorization context is carried by the User ↔ Organization
     * relationship, not by the Role name itself.</p>
     */
    @Override
    @Transactional
    public Role findOrCreate(String name, String description) {
        return roleRepository.findByName(name)
            .orElseGet(() -> {
                log.info("Role '{}' not found in store — creating and persisting.", name);
                Role newRole = Role.builder()
                    .name(name)
                    .description(description)
                    .build();
                return roleRepository.save(newRole);
            });
    }
}
