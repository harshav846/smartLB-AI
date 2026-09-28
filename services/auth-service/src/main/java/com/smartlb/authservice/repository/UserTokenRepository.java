package com.smartlb.authservice.repository;

import com.smartlb.authservice.entity.UserToken;
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
 * Repository interface for {@link UserToken} entity operations.
 */
@Repository
public interface UserTokenRepository extends JpaRepository<UserToken, UUID> {

    /**
     * Finds an unused single-use token by its SHA-256 hash and token type.
     *
     * @param tokenHash hashed token string
     * @param tokenType type category (e.g. EMAIL_VERIFICATION, PASSWORD_RESET)
     * @return Optional enclosing UserToken if found and unused
     */
    Optional<UserToken> findByTokenHashAndTokenTypeAndUsedFalse(String tokenHash, String tokenType);

    /**
     * Locates all tokens for a user and token type.
     *
     * @param userId    user UUID
     * @param tokenType type category
     * @return list of UserToken entities
     */
    List<UserToken> findByUserIdAndTokenType(UUID userId, String tokenType);

    /**
     * Atomically marks all unused tokens of a specific type as used for a user.
     *
     * @param userId    user UUID
     * @param tokenType type category
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE UserToken t SET t.used = true WHERE t.user.id = :userId AND t.tokenType = :tokenType AND t.used = false")
    void invalidateAllByUserIdAndTokenType(@Param("userId") UUID userId, @Param("tokenType") String tokenType);

    /**
     * Deletes expired user tokens.
     *
     * @param cutoffDateTime cutoff timestamp
     */
    void deleteByExpiryDateBefore(OffsetDateTime cutoffDateTime);
}
