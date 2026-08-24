package com.tuhospedaje.repository;

import java.time.Instant;

public interface EmailOutboxClaimRepository {

    int claimEligible(String emailType, Instant now, int batchSize, String token, Instant leaseUntil);
}
