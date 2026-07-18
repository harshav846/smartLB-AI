package com.smartlb.authservice.mapper;

import com.smartlb.authservice.entity.Role;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper for converting Role entity structures.
 */
@Mapper(componentModel = "spring")
public interface RoleMapper {

    /**
     * Maps Role entity to name.
     * @param role the source Entity
     * @return the name string representation of the role
     */
    default String toName(Role role) {
        if (role == null) {
            return null;
        }
        return role.getName();
    }

    /**
     * Maps name string to Role entity container.
     * @param name input name
     * @return Role container object
     */
    default Role toRole(String name) {
        if (name == null) {
            return null;
        }
        Role role = new Role();
        role.setName(name);
        return role;
    }
}
