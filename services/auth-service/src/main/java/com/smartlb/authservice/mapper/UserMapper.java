package com.smartlb.authservice.mapper;

import com.smartlb.authservice.dto.request.RegisterOrganizationRequest;
import com.smartlb.authservice.dto.response.UserProfileResponse;
import com.smartlb.authservice.entity.User;
import com.smartlb.authservice.entity.UserRole;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * MapStruct mapper for converting User entity and profile DTO structures.
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    /**
     * Converts User Entity to UserProfileResponse DTO.
     * Maps nested fields (organization identifiers) and converts the UserRoles relationship set into role strings.
     * @param user the source Entity
     * @return the destination Response DTO
     */
    @Mapping(target = "userId", source = "id")
    @Mapping(target = "organizationId", source = "organization.id")
    @Mapping(target = "organizationName", source = "organization.name")
    @Mapping(target = "roles", source = "userRoles")
    UserProfileResponse toResponse(User user);

    /**
     * Converts UserProfileResponse DTO to User Entity.
     * Ignores passwords and intermediate relations.
     * @param response the source Response DTO
     * @return the destination Entity
     */
    @Mapping(target = "id", source = "userId")
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "userRoles", ignore = true)
    @Mapping(target = "failedLoginAttempts", ignore = true)
    @Mapping(target = "lastPasswordChange", ignore = true)
    @Mapping(target = "lastLogin", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    User toEntity(UserProfileResponse response);

    /**
     * Converts RegisterOrganizationRequest DTO to User Entity (representing the initial administrator user).
     * Maps 'adminFirstName' to 'firstName', 'adminLastName' to 'lastName', etc.
     * @param request the source Request DTO
     * @return the destination Entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "firstName", source = "adminFirstName")
    @Mapping(target = "lastName", source = "adminLastName")
    @Mapping(target = "email", source = "adminEmail")
    @Mapping(target = "passwordHash", source = "adminPassword")
    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "userRoles", ignore = true)
    @Mapping(target = "emailVerified", ignore = true)
    @Mapping(target = "failedLoginAttempts", ignore = true)
    @Mapping(target = "lastPasswordChange", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "lastLogin", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    User toEntity(RegisterOrganizationRequest request);

    /**
     * Custom collection mapper mapping user roles set to list of name strings.
     * @param userRoles the source Set
     * @return the destination List
     */
    default List<String> mapRoles(Set<UserRole> userRoles) {
        if (userRoles == null) {
            return null;
        }
        return userRoles.stream()
                .map(userRole -> userRole.getRole().getName())
                .collect(Collectors.toList());
    }
}
