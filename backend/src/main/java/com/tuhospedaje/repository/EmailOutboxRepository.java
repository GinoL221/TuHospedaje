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
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailOutboxRepository extends JpaRepository<EmailOutbox, Long>, EmailOutboxClaimRepository {

    Optional<EmailOutbox> findByEmailTypeAndAggregateId(String emailType, String aggregateId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE EmailOutbox o
            SET o.status = 'PENDING', o.failedAttempts = 0, o.nextAttemptAt = NULL,
                o.leaseToken = NULL, o.leaseUntil = NULL, o.errorCode = NULL, o.completedAt = NULL
            WHERE o.emailType = 'WELCOME' AND o.aggregateId = :aggregateId
              AND o.status IN ('DELIVERED', 'FAILED') AND o.completedAt <= :cooldownCutoff
            """)
    int requeueWelcomeIfTerminalAndCooled(@Param("aggregateId") String aggregateId,
                                          @Param("cooldownCutoff") Instant cooldownCutoff);

    @Query("SELECT o FROM EmailOutbox o WHERE o.status = :status AND o.leaseToken = :token ORDER BY o.id ASC")
    List<EmailOutbox> findByStatusAndLeaseToken(@Param("status") EmailOutboxStatus status,
                                                @Param("token") String token);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE EmailOutbox o
            SET o.status = 'DELIVERED', o.completedAt = :completedAt, o.leaseToken = NULL, o.leaseUntil = NULL
            WHERE o.id = :id AND o.status = 'PROCESSING' AND o.leaseToken = :token
              AND o.leaseUntil > :completedAt
            """)
    int markDelivered(@Param("id") Long id,
                      @Param("token") String token,
                      @Param("completedAt") Instant completedAt);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE EmailOutbox o
            SET o.status = 'FAILED', o.completedAt = :completedAt, o.errorCode = :errorCode,
                o.failedAttempts = o.failedAttempts + 1, o.leaseToken = NULL, o.leaseUntil = NULL
            WHERE o.id = :id AND o.status = 'PROCESSING' AND o.leaseToken = :token
              AND o.leaseUntil > :completedAt
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
                o.nextAttemptAt = :nextAttemptAt,
                o.leaseToken = NULL,
                o.leaseUntil = NULL,
                o.errorCode = :errorCode
            WHERE o.id = :id AND o.status = 'PROCESSING' AND o.leaseToken = :token
              AND o.leaseUntil > CURRENT_TIMESTAMP
            """)
    int releaseForRetry(@Param("id") Long id,
                        @Param("token") String token,
                        @Param("errorCode") String errorCode,
                        @Param("nextAttemptAt") Instant nextAttemptAt);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            DELETE FROM EmailOutbox o
            WHERE o.emailType = :emailType AND o.status IN ('DELIVERED', 'FAILED') AND o.completedAt < :cutoff
            """)
    int purgeCompletedBefore(@Param("emailType") String emailType, @Param("cutoff") Instant cutoff);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            DELETE FROM EmailOutbox o
            WHERE o.emailType = 'WELCOME' AND o.status IN ('DELIVERED', 'FAILED') AND o.completedAt < :cutoff
            """)
    int purgeWelcomeCompletedBefore(@Param("cutoff") Instant cutoff);

    default List<EmailOutbox> claimEligible(Instant now, int batchSize) {
        String token = UUID.randomUUID().toString();
        Instant leaseUntil = now.plus(java.time.Duration.ofMinutes(5));
        claimEligible("WELCOME", now, batchSize, token, leaseUntil);
        return findByStatusAndLeaseToken(EmailOutboxStatus.PROCESSING, token);
    }
}
