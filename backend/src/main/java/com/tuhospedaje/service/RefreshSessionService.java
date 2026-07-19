package com.tuhospedaje.service;

import com.tuhospedaje.entity.User;

import java.time.Instant;

public interface RefreshSessionService {
    Session issue(User user);

    Session rotate(String refreshCredential);

    void revokeCurrent(String refreshCredential);

    void revokeAll(long userId, String reason);

    record Session(long familyId, String refreshCredential, Instant absoluteExpiresAt) {
    }

    class Rejected extends RuntimeException {
        public Rejected() {
            super("Refresh credential rejected");
        }
    }
}
