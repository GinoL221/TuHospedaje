package com.tuhospedaje.repository;

import com.tuhospedaje.entity.RefreshTokenFamily;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface RefreshTokenFamilyRepository extends JpaRepository<RefreshTokenFamily, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT family FROM RefreshTokenFamily family WHERE family.id = :id")
    Optional<RefreshTokenFamily> findByIdForUpdate(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE RefreshTokenFamily family
            SET family.revokedAt = :revokedAt, family.revocationReason = :reason
            WHERE family.user.id = :userId AND family.revokedAt IS NULL
            """)
    int revokeActiveFamiliesForUser(@Param("userId") Long userId,
                                    @Param("revokedAt") Instant revokedAt,
                                    @Param("reason") String reason);
}
