package com.smartlb.authservice.service;

import com.smartlb.authservice.dto.request.RegisterOrganizationRequest;
import com.smartlb.authservice.dto.response.OrganizationResponse;
import com.smartlb.authservice.entity.Organization;
import java.util.UUID;

/**
 * Service interface for managing tenant Organization entity lifecycle, lookup queries,
 * and uniqueness verifications.
 */
public interface OrganizationService {

    /**
     * Creates and persists a new tenant Organization entity.
     *
     * @param request registration request containing organization attributes
     * @return persisted Organization entity
     */
    Organization createOrganization(RegisterOrganizationRequest request);

    /**
     * Retrieves tenant organization response details by unique identifier.
     *
     * @param id unique UUID identifier of the organization
     * @return OrganizationResponse DTO representation
     */
    OrganizationResponse getOrganizationById(UUID id);

    /**
     * Retrieves tenant organization response details by URL slug.
     *
     * @param slug organization unique URL slug string
     * @return OrganizationResponse DTO representation
     */
    OrganizationResponse getOrganizationBySlug(String slug);

    /**
     * Internal domain helper method retrieving the raw Organization entity by ID.
     *
     * @param id unique UUID identifier of the organization
     * @return Organization domain entity
     */
    Organization getEntityById(UUID id);

    /**
     * Checks if an organization exists matching the specified URL slug.
     *
     * @param slug organization URL slug segment
     * @return true if exists, false otherwise
     */
    boolean existsBySlug(String slug);

    /**
     * Checks if an organization exists matching the specified company name.
     *
     * @param name company name string
     * @return true if exists, false otherwise
     */
    boolean existsByName(String name);
}
