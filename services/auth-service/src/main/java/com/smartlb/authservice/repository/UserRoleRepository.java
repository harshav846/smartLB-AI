package com.smartlb.authservice.repository;

import com.smartlb.authservice.entity.UserRole;
import com.smartlb.authservice.entity.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for UserRole junction entity operations.
 */
@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

    /**
     * Finds all role mappings assigned to a specific user.
     * @param userId unique user key
     * @return list of UserRole associations
     */
    List<UserRole> findByUserId(UUID userId);

    /**
     * Revokes all roles currently mapped to a user.
     * Used principally during account wipes or roles resets.
     * @param userId unique user key
     */
    void deleteByUserId(UUID userId);
}
