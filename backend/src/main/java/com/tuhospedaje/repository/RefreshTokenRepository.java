package com.tuhospedaje.repository;

import com.tuhospedaje.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.time.Instant;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByHmacKeyIdAndTokenHmac(String hmacKeyId, byte[] tokenHmac);
    @Lock(LockModeType.PESSIMISTIC_READ)
    Optional<RefreshToken> findByPredecessorId(Long predecessorId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT token FROM RefreshToken token WHERE token.id = :id")
    Optional<RefreshToken> findByIdForUpdate(@Param("id") Long id);
    long countByFamilyId(Long familyId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE RefreshToken token SET token.revokedAt = :revokedAt WHERE token.family.id = :familyId AND token.revokedAt IS NULL")
    int revokeAllForFamily(Long familyId, Instant revokedAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE refresh_tokens AS token
            JOIN refresh_token_families AS family ON family.id = token.family_id
            SET token.revoked_at = :revokedAt
            WHERE family.user_id = :userId AND token.revoked_at IS NULL
            """, nativeQuery = true)
    int revokeActiveTokensForUser(@Param("userId") Long userId, @Param("revokedAt") Instant revokedAt);
}
