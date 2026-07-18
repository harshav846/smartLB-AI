package com.smartlb.authservice.mapper;

import com.smartlb.authservice.dto.request.RegisterOrganizationRequest;
import com.smartlb.authservice.dto.response.OrganizationResponse;
import com.smartlb.authservice.entity.Organization;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for converting Organization entity and corresponding DTO structures.
 */
@Mapper(componentModel = "spring")
public interface OrganizationMapper {

    /**
     * Converts Organization Entity to OrganizationResponse DTO.
     * @param organization the source Entity
     * @return the destination Response DTO
     */
    OrganizationResponse toResponse(Organization organization);

    /**
     * Converts OrganizationResponse DTO to Organization Entity.
     * @param response the source Response DTO
     * @return the destination Entity
     */
    @Mapping(target = "users", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Organization toEntity(OrganizationResponse response);

    /**
     * Converts RegisterOrganizationRequest DTO to Organization Entity.
     * Maps 'organizationName' to 'name', 'organizationSlug' to 'slug'.
     * @param request the source Request DTO
     * @return the destination Entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "name", source = "organizationName")
    @Mapping(target = "slug", source = "organizationSlug")
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "subscriptionType", ignore = true)
    @Mapping(target = "users", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Organization toEntity(RegisterOrganizationRequest request);
}
