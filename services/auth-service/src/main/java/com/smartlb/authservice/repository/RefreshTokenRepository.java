package com.smartlb.authservice.repository;

import com.smartlb.authservice.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for {@link RefreshToken} entity operations.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Locates a refresh token entity by its SHA-256 token hash.
     *
     * @param tokenHash hashed token string
     * @return Optional enclosing RefreshToken if found
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Finds all non-revoked active refresh tokens for a user.
     *
     * @param userId user UUID
     * @return list of active RefreshToken entities
     */
    List<RefreshToken> findByUserIdAndRevokedFalse(UUID userId);

    /**
     * Atomically revokes all active refresh tokens mapped to a user ID.
     *
     * @param userId user UUID
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.user.id = :userId AND r.revoked = false")
    void revokeAllByUserId(@Param("userId") UUID userId);

    /**
     * Purges expired refresh tokens from the database.
     *
     * @param cutoffDateTime cutoff timestamp
     */
    void deleteByExpiryDateBefore(OffsetDateTime cutoffDateTime);
}
