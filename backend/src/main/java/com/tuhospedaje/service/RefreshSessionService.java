package com.tuhospedaje.service;

import com.tuhospedaje.entity.User;

import java.time.Instant;

public interface RefreshSessionService {
    Session issue(User user);

    Session rotate(String refreshCredential);

    void revokeCurrent(String refreshCredential);

    void revokeAll(long userId, String reason);

    /**
     * {@code userId} is additive (Design ADR-2, PR1/WU2): the HTTP {@code /refresh}
     * orchestration needs the owning user's id to mint a new access JWT without an
     * extra lookup by familyId. Existing callers only read accessors, so this is
     * source-compatible with every prior {@code Session} consumer.
     */
    record Session(long familyId, String refreshCredential, Instant absoluteExpiresAt, long userId) {
    }

    class Rejected extends RuntimeException {
        public Rejected() {
            super("Refresh credential rejected");
        }
    }
}
