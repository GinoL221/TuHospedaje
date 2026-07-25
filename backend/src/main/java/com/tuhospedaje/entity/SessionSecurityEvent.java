package com.tuhospedaje.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "session_security_events")
public class SessionSecurityEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_id")
    private RefreshTokenFamily family;

    @Column(nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private Type eventType;

    @Column(nullable = false, columnDefinition = "DATETIME(6)")
    private Instant occurredAt;

    @Column(nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    private DeliveryState deliveryState;

    @Column(nullable = false)
    private int deliveryAttempts;

    @Column(columnDefinition = "DATETIME(6)")
    private Instant nextAttemptAt;

    @Column(columnDefinition = "DATETIME(6)")
    private Instant deliveredAt;

    @Column(length = 64)
    private String lastErrorCode;

    public enum Type {
        REFRESH_REUSE,
        ADMIN_DISABLE,
        PASSWORD_CHANGE,
        LOGOUT_ALL
    }

    public enum DeliveryState {
        PENDING
    }
}
