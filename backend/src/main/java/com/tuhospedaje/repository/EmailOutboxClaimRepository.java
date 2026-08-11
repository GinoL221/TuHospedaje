package com.tuhospedaje.repository;

import java.time.Instant;

public interface EmailOutboxClaimRepository {

    int claimEligible(Instant now, int batchSize, String token, Instant leaseUntil);
}
