package com.smartlb.authservice.repository;

import com.smartlb.authservice.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Organization entity operations.
 */
@Repository
public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    /**
     * Checks if an organization exists with the specified slug.
     * @param slug the organization URL slug segment
     * @return true if exists, false otherwise
     */
    boolean existsBySlug(String slug);

    /**
     * Checks if an organization exists with the specified name.
     * @param name the company name
     * @return true if exists, false otherwise
     */
    boolean existsByName(String name);

    /**
     * Finds an organization by its unique name slug.
     * @param slug the url name segment
     * @return an Optional enclosing the organization if found
     */
    Optional<Organization> findBySlug(String slug);

    /**
     * Finds an active organization by ID verify it has not been soft-deleted.
     * @param id the organization UUID key
     * @return an Optional enclosing the organization if active and found
     */
    Optional<Organization> findByIdAndDeletedAtIsNull(UUID id);
}
