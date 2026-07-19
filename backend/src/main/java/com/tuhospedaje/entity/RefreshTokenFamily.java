package com.tuhospedaje.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "refresh_token_families")
public class RefreshTokenFamily {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "family_uuid", nullable = false, unique = true, columnDefinition = "BINARY(16)")
    @JdbcTypeCode(Types.BINARY)
    private UUID familyUuid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private long currentGeneration;

    @Column(nullable = false, columnDefinition = "DATETIME(6)")
    private Instant issuedAt;

    @Column(nullable = false, columnDefinition = "DATETIME(6)")
    private Instant absoluteExpiresAt;

    @Column(columnDefinition = "DATETIME(6)")
    private Instant lastRotatedAt;

    @Column(columnDefinition = "DATETIME(6)")
    private Instant lastSeenAt;

    @Column(columnDefinition = "DATETIME(6)")
    private Instant revokedAt;

    @Column(length = 32)
    private String revocationReason;

    @Column(columnDefinition = "DATETIME(6)")
    private Instant reuseDetectedAt;

    @Column(columnDefinition = "BINARY(32)")
    private byte[] createdIpHash;

    @Column(columnDefinition = "BINARY(32)")
    private byte[] createdUserAgentHash;

    @Version
    @Column(nullable = false)
    private Long version;
}
