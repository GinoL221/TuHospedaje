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
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT token FROM RefreshToken token WHERE token.id = :id")
    Optional<RefreshToken> findByIdForUpdate(@Param("id") Long id);
    long countByFamilyId(Long familyId);

    @Modifying
    @Query("UPDATE RefreshToken token SET token.revokedAt = :revokedAt WHERE token.family.id = :familyId AND token.revokedAt IS NULL")
    int revokeAllForFamily(Long familyId, Instant revokedAt);
}
