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
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "family_id", nullable = false)
    private RefreshTokenFamily family;

    @Column(nullable = false)
    private long generation;

    @Column(name = "token_hmac", nullable = false, columnDefinition = "BINARY(32)")
    private byte[] tokenHmac;

    @Column(nullable = false, length = 32)
    private String hmacKeyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "predecessor_token_id", unique = true)
    private RefreshToken predecessor;

    @Column(nullable = false, columnDefinition = "DATETIME(6)")
    private Instant issuedAt;

    @Column(nullable = false, columnDefinition = "DATETIME(6)")
    private Instant expiresAt;

    @Column(columnDefinition = "DATETIME(6)")
    private Instant consumedAt;

    @Column(columnDefinition = "DATETIME(6)")
    private Instant revokedAt;

    @Column(columnDefinition = "DATETIME(6)")
    private Instant lastPresentedAt;
}
