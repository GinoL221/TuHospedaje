package com.tuhospedaje.repository;

import com.tuhospedaje.entity.EmailOutbox;
import com.tuhospedaje.enums.EmailOutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface EmailOutboxRepository extends JpaRepository<EmailOutbox, Long> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            UPDATE email_outbox
            SET status = 'PROCESSING',
                lease_token = :token,
                lease_until = :leaseUntil
            WHERE id IN (
                SELECT id FROM (
                    SELECT id FROM email_outbox
                    WHERE status = 'PENDING'
                       OR (status = 'PROCESSING' AND lease_until < :now)
                    ORDER BY id ASC
                    LIMIT :batchSize
                ) AS batch
            )
            """, nativeQuery = true)
    int claimEligible(@Param("now") Instant now,
                      @Param("batchSize") int batchSize,
                      @Param("token") String token,
                      @Param("leaseUntil") Instant leaseUntil);

    @Query("SELECT o FROM EmailOutbox o WHERE o.status = :status AND o.leaseToken = :token ORDER BY o.id ASC")
    List<EmailOutbox> findByStatusAndLeaseToken(@Param("status") EmailOutboxStatus status,
                                                @Param("token") String token);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE EmailOutbox o
            SET o.status = 'DELIVERED', o.completedAt = :completedAt
            WHERE o.id = :id AND o.status = 'PROCESSING' AND o.leaseToken = :token
            """)
    int markDelivered(@Param("id") Long id,
                      @Param("token") String token,
                      @Param("completedAt") Instant completedAt);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE EmailOutbox o
            SET o.status = 'FAILED', o.completedAt = :completedAt, o.errorCode = :errorCode
            WHERE o.id = :id AND o.status = 'PROCESSING' AND o.leaseToken = :token
            """)
    int markFailed(@Param("id") Long id,
                   @Param("token") String token,
                   @Param("completedAt") Instant completedAt,
                   @Param("errorCode") String errorCode);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE EmailOutbox o
            SET o.status = 'PENDING',
                o.failedAttempts = o.failedAttempts + 1,
                o.leaseToken = NULL,
                o.leaseUntil = NULL,
                o.errorCode = :errorCode
            WHERE o.id = :id AND o.status = 'PROCESSING' AND o.leaseToken = :token
            """)
    int releaseForRetry(@Param("id") Long id,
                        @Param("token") String token,
                        @Param("errorCode") String errorCode);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            DELETE FROM EmailOutbox o
            WHERE o.status = 'DELIVERED' AND o.completedAt < :cutoff
            """)
    int purgeCompletedBefore(@Param("cutoff") Instant cutoff);

    default List<EmailOutbox> claimEligible(Instant now, int batchSize) {
        String token = UUID.randomUUID().toString();
        Instant leaseUntil = now.plus(java.time.Duration.ofMinutes(5));
        claimEligible(now, batchSize, token, leaseUntil);
        return findByStatusAndLeaseToken(EmailOutboxStatus.PROCESSING, token);
    }
}
