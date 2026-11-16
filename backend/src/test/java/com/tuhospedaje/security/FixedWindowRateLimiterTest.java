package com.tuhospedaje.security;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Design "Extract FixedWindowRateLimiter, but do NOT migrate AuthRateLimitFilter onto
 * it": the algorithm is verbatim from {@code AuthRateLimitFilter}'s bucket/compute/lazy
 * sweep (see {@code AuthRateLimitFilterTest} for the precedent this mirrors), extracted
 * behind an {@link Instant}-based API and a package-visible {@link
 * FixedWindowRateLimiter#trackedKeys()} so the sweep test needs no reflection.
 */
class FixedWindowRateLimiterTest {

    private final FixedWindowRateLimiter limiter = new FixedWindowRateLimiter();

    // --- LIM-1/2: limit boundary ---

    @Test
    void nthCallAtTheLimitReturnsFalse() {
        Instant now = Instant.ofEpochSecond(0L);

        assertThat(limiter.exceeds("key", now, 2)).isFalse();
        assertThat(limiter.exceeds("key", now, 2)).isFalse();
    }

    @Test
    void limitPlusOneInTheSameMinuteReturnsTrue() {
        Instant now = Instant.ofEpochSecond(0L);

        assertThat(limiter.exceeds("key", now, 2)).isFalse();
        assertThat(limiter.exceeds("key", now, 2)).isFalse();

        assertThat(limiter.exceeds("key", now, 2)).isTrue();
    }

    // --- LIM-3: window rollover ---

    @Test
    void theNextMinuteResetsTheCounter() {
        Instant firstMinute = Instant.ofEpochSecond(0L);
        Instant nextMinute = Instant.ofEpochSecond(61L);

        assertThat(limiter.exceeds("key", firstMinute, 1)).isFalse();
        assertThat(limiter.exceeds("key", firstMinute, 1)).isTrue();

        assertThat(limiter.exceeds("key", nextMinute, 1)).isFalse();
    }

    // --- LIM-4: distinct keys are independent ---

    @Test
    void distinctKeysAreIndependent() {
        Instant now = Instant.ofEpochSecond(0L);

        assertThat(limiter.exceeds("key-a", now, 1)).isFalse();
        assertThat(limiter.exceeds("key-a", now, 1)).isTrue();

        assertThat(limiter.exceeds("key-b", now, 1)).isFalse();
    }

    // --- LIM-5: retryAfterSeconds formula ---

    @Test
    void retryAfterSecondsIsSixtyMinusEpochModuloSixty() {
        assertThat(FixedWindowRateLimiter.retryAfterSeconds(Instant.ofEpochSecond(0L))).isEqualTo(60L);
        assertThat(FixedWindowRateLimiter.retryAfterSeconds(Instant.ofEpochSecond(1L))).isEqualTo(59L);
        assertThat(FixedWindowRateLimiter.retryAfterSeconds(Instant.ofEpochSecond(30L))).isEqualTo(30L);
    }

    @Test
    void retryAfterSecondsIsFlooredAtOne() {
        // epoch % 60 == 59 -> 60 - 59 == 1, the smallest non-degenerate value; the
        // formula must never reach or cross zero.
        assertThat(FixedWindowRateLimiter.retryAfterSeconds(Instant.ofEpochSecond(59L))).isEqualTo(1L);
    }

    // --- LIM-6: bounded eviction sweep — evicts stale, retains a live current-minute
    // bucket. Ported from AuthRateLimitFilterTest's just-merged triangulation
    // (sweepEvictsStaleBucketsButRetainsCurrentMinuteBucketsAfterTheWindowRolls,
    // commits e2c06e/631d115), asserted from the start via trackedKeys() instead of
    // reflection.

    @Test
    void sweepEvictsStaleBucketsButRetainsCurrentMinuteBucketsAfterTheWindowRolls() {
        Instant staleMinute = Instant.ofEpochSecond(0L);

        // Exactly MAX_TRACKED_KEYS (10_000) distinct keys at minute 0 — right at the
        // sweep threshold, not yet over it, all now-stale once the clock advances.
        for (int i = 0; i < 10_000; i++) {
            limiter.exceeds("key-" + i, staleMinute, 1000);
        }
        assertThat(limiter.trackedKeys()).isEqualTo(10_000);

        Instant currentMinute = Instant.ofEpochSecond(61L);

        // A live bucket at the CURRENT minute, seeded before the map crosses the sweep
        // threshold: this call's own maybeSweep still sees size == 10_000 (not yet
        // "> 10_000"), so no sweep runs here — it only adds this one live entry. Limit 1
        // means this first call lands exactly at the boundary (count=1, not exceeded).
        assertThat(limiter.exceeds("live-key", currentMinute, 1)).isFalse();
        assertThat(limiter.trackedKeys()).isEqualTo(10_001);

        // This second, distinct call is the one whose maybeSweep sees 10_001 > 10_000
        // and actually triggers the sweep.
        limiter.exceeds("other-live-key", currentMinute, 1000);

        assertThat(limiter.trackedKeys())
                .as("the sweep must evict every stale (minute 0) bucket, but a naive "
                        + "`removeIf(true)` or off-by-one predicate would also delete the "
                        + "current-minute live bucket seeded above")
                .isLessThan(10);
        assertThat(limiter.exceeds("live-key", currentMinute, 1))
                .as("live-key's bucket must have survived the sweep with count=1 intact — a "
                        + "third hit against limit=1 must now exceed (count=2), proving the "
                        + "bucket was retained rather than reset to a fresh count=1")
                .isTrue();
    }

    // --- LIM-7: attempt budget across a real-minute rollover (pigeonhole bound) ---
    //
    // The integration test AuthRateLimitIntegrationTest drives real HTTP attempts
    // against the real clock, so a minute boundary can land anywhere inside its
    // attempt sequence and split it across two windows as k + (total - k). These
    // tests pin, with explicit Instants and no real clock, that exactly
    // 2 x ceiling attempts FAIL at the split k = ceiling (ceiling + ceiling, neither
    // window exceeds — the flaky failure mode of issue #469), while 2 x ceiling + 1
    // attempts exceed for EVERY possible split position, per pigeonhole.

    /**
     * Replays a sequence of {@code total} attempts against a fresh limiter, with the
     * first {@code splitAt} attempts in minute M (epoch second 0) and the rest in
     * minute M+1 (epoch second 61), and reports whether ANY attempt was blocked.
     */
    private boolean anyAttemptExceedsAcrossRollover(int ceiling, int total, int splitAt) {
        FixedWindowRateLimiter rolloverLimiter = new FixedWindowRateLimiter();
        Instant minuteM = Instant.ofEpochSecond(0L);
        Instant minuteMPlusOne = Instant.ofEpochSecond(61L);
        for (int i = 1; i <= total; i++) {
            Instant now = i <= splitAt ? minuteM : minuteMPlusOne;
            if (rolloverLimiter.exceeds("key", now, ceiling)) {
                return true;
            }
        }
        return false;
    }

    @Test
    void twoTimesCeilingAttemptsNeverExceedWhenTheRolloverSplitsExactlyAtTheCeiling() {
        // The failure mode behind the flaky integration test: 6 attempts against a
        // ceiling of 3 with the boundary landing after attempt 3 split the sequence
        // 3 + 3 — each window reaches exactly the ceiling and neither exceeds, so no
        // 429 ever appears. The same evenly-split hole exists for every ceiling.
        assertThat(anyAttemptExceedsAcrossRollover(3, 2 * 3, 3))
                .as("2 x 3 attempts split 3 + 3 across the rollover must NOT trip a ceiling of 3 "
                        + "— this is exactly why the old integration budget of 6 was flaky")
                .isFalse();
        assertThat(anyAttemptExceedsAcrossRollover(2, 2 * 2, 2))
                .as("2 x 2 attempts split 2 + 2 must NOT trip a ceiling of 2")
                .isFalse();
        assertThat(anyAttemptExceedsAcrossRollover(1, 2 * 1, 1))
                .as("2 x 1 attempts split 1 + 1 must NOT trip a ceiling of 1")
                .isFalse();
    }

    @Test
    void twoTimesCeilingAttemptsDoExceedAtEveryOtherSplitPosition() {
        // Triangulation: the old budget is not universally broken — only the exact
        // even split k = ceiling slips through. Every other rollover position gives
        // one window more than the ceiling.
        for (int splitAt = 0; splitAt <= 2 * 3; splitAt++) {
            if (splitAt == 3) {
                continue;
            }
            assertThat(anyAttemptExceedsAcrossRollover(3, 2 * 3, splitAt))
                    .as("with 2 x 3 attempts and the rollover after attempt %d, one window "
                            + "receives more than 3 attempts and must exceed", splitAt)
                    .isTrue();
        }
    }

    @Test
    void twoTimesCeilingPlusOneAttemptsExceedForEveryRolloverPosition() {
        // Pigeonhole: total = 2 x ceiling + 1 attempts over two windows forces one
        // window to receive at least ceiling + 1 attempts, wherever the boundary
        // falls — including the boundary-free splits k = 0 and k = total (all
        // attempts land in a single window).
        int[] ceilings = {3, 2, 1};
        for (int ceiling : ceilings) {
            int total = 2 * ceiling + 1;
            for (int splitAt = 0; splitAt <= total; splitAt++) {
                assertThat(anyAttemptExceedsAcrossRollover(ceiling, total, splitAt))
                        .as("ceiling %d with %d attempts and the rollover after attempt %d "
                                        + "must exceed for every possible split position",
                                ceiling, total, splitAt)
                        .isTrue();
            }
        }
    }
}
