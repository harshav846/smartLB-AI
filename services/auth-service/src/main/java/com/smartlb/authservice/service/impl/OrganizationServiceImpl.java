package com.smartlb.authservice.service.impl;

import com.smartlb.authservice.dto.response.OrganizationResponse;
import com.smartlb.authservice.dto.request.RegisterOrganizationRequest;
import com.smartlb.authservice.entity.Organization;
import com.smartlb.authservice.exception.DuplicateResourceException;
import com.smartlb.authservice.exception.OrganizationNotFoundException;
import com.smartlb.authservice.mapper.OrganizationMapper;
import com.smartlb.authservice.repository.OrganizationRepository;
import com.smartlb.authservice.service.OrganizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

/**
 * Service implementation for tenant Organization entity lifecycle management.
 */
@Service
@Transactional(readOnly = true)
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMapper organizationMapper;

    public OrganizationServiceImpl(OrganizationRepository organizationRepository,
                                   OrganizationMapper organizationMapper) {
        this.organizationRepository = organizationRepository;
        this.organizationMapper = organizationMapper;
    }

    /**
     * {@inheritDoc}
     * Validates slug and name uniqueness, then persists the new Organization entity.
     *
     * @throws DuplicateResourceException if slug or name already exists
     */
    @Override
    @Transactional
    public Organization createOrganization(RegisterOrganizationRequest request) {
        if (organizationRepository.existsBySlug(request.getOrganizationSlug())) {
            throw new DuplicateResourceException(
                "Organization slug '" + request.getOrganizationSlug() + "' is already taken.");
        }
        if (organizationRepository.existsByName(request.getOrganizationName())) {
            throw new DuplicateResourceException(
                "Organization name '" + request.getOrganizationName() + "' is already registered.");
        }
        Organization organization = organizationMapper.toEntity(request);
        return organizationRepository.save(organization);
    }

    /**
     * {@inheritDoc}
     *
     * @throws OrganizationNotFoundException if no organization is found with the given ID
     */
    @Override
    public OrganizationResponse getOrganizationById(UUID id) {
        return organizationRepository.findByIdAndDeletedAtIsNull(id)
            .map(organizationMapper::toResponse)
            .orElseThrow(() -> new OrganizationNotFoundException(
                "Organization not found with ID: " + id));
    }

    /**
     * {@inheritDoc}
     *
     * @throws OrganizationNotFoundException if no organization is found with the given slug
     */
    @Override
    public OrganizationResponse getOrganizationBySlug(String slug) {
        return organizationRepository.findBySlug(slug)
            .map(organizationMapper::toResponse)
            .orElseThrow(() -> new OrganizationNotFoundException(
                "Organization not found with slug: " + slug));
    }

    /**
     * {@inheritDoc}
     *
     * @throws OrganizationNotFoundException if no entity is found with the given ID
     */
    @Override
    public Organization getEntityById(UUID id) {
        return organizationRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new OrganizationNotFoundException(
                "Organization not found with ID: " + id));
    }

    /** {@inheritDoc} */
    @Override
    public boolean existsBySlug(String slug) {
        return organizationRepository.existsBySlug(slug);
    }

    /** {@inheritDoc} */
    @Override
    public boolean existsByName(String name) {
        return organizationRepository.existsByName(name);
    }
}
