package com.tuhospedaje.repository;

import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

public class EmailOutboxRepositoryImpl implements EmailOutboxClaimRepository {

    private static final String SELECT_ELIGIBLE_IDS = """
            SELECT id
            FROM email_outbox
            WHERE (status = 'PENDING'
                   AND (next_attempt_at IS NULL OR next_attempt_at <= :now))
               OR (status = 'PROCESSING' AND lease_until < :now)
            ORDER BY id ASC
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """;

    private static final String CLAIM_IDS = """
            UPDATE email_outbox
            SET status = 'PROCESSING',
                lease_token = :token,
                lease_until = :leaseUntil
            WHERE id IN (:ids)
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final EntityManager entityManager;

    public EmailOutboxRepositoryImpl(NamedParameterJdbcTemplate jdbcTemplate, EntityManager entityManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public int claimEligible(Instant now, int batchSize, String token, Instant leaseUntil) {
        entityManager.flush();
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("now", mariaDbDateTime(now))
                .addValue("batchSize", batchSize);
        List<Long> ids = jdbcTemplate.queryForList(SELECT_ELIGIBLE_IDS, parameters, Long.class);
        if (ids.isEmpty()) {
            return 0;
        }

        int claimed = jdbcTemplate.update(CLAIM_IDS, new MapSqlParameterSource()
                .addValue("ids", ids)
                .addValue("token", token)
                .addValue("leaseUntil", mariaDbDateTime(leaseUntil)));
        entityManager.clear();
        return claimed;
    }

    private Timestamp mariaDbDateTime(Instant instant) {
        return Timestamp.valueOf(LocalDateTime.ofInstant(instant, ZoneOffset.UTC));
    }
}
