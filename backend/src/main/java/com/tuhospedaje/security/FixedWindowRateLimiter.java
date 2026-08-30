package com.tuhospedaje.security;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Design "Extract FixedWindowRateLimiter, but do NOT migrate AuthRateLimitFilter onto
 * it": a shared, in-process fixed-window attempt counter with a lazy bounded eviction
 * sweep. The algorithm is verbatim from {@code AuthRateLimitFilter}'s bucket/compute
 * pattern; it is extracted here because this change adds a second and third consumer
 * (the refresh IP filter, and — in a follow-up PR — the refresh family ceiling), and a
 * fixed-window counter is security-critical code that must not drift between multiple
 * hand-copies.
 *
 * <p>{@code AuthRateLimitFilter} itself is deliberately NOT migrated onto this class:
 * {@code AuthRateLimitFilterTest} reads its private {@code buckets} field by reflection,
 * so delegating would break that just-merged suite. See the design doc's migration
 * follow-up.
 */
public final class FixedWindowRateLimiter {

    private static final int MAX_TRACKED_KEYS = 10_000;

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final AtomicBoolean sweeping = new AtomicBoolean(false);

    private record Bucket(long minute, int count) {
    }

    /**
     * Atomic per-key compute; a stale bucket for the current key resets itself on the
     * next hit for that key, no extra lock required.
     */
    public boolean exceeds(String key, Instant now, int limit) {
        long minute = now.getEpochSecond() / 60;
        maybeSweep(minute);
        Bucket updated = buckets.compute(key, (k, prev) ->
                (prev == null || prev.minute() != minute)
                        ? new Bucket(minute, 1)
                        : new Bucket(minute, prev.count() + 1));
        return updated.count() > limit;
    }

    /**
     * Lazy bounded sweep: abandoned keys are swept inline when the map grows past
     * {@link #MAX_TRACKED_KEYS}, guarded by an {@link AtomicBoolean} so exactly one
     * thread performs the sweep.
     */
    private void maybeSweep(long minute) {
        if (buckets.size() > MAX_TRACKED_KEYS && sweeping.compareAndSet(false, true)) {
            try {
                buckets.entrySet().removeIf(e -> e.getValue().minute() < minute);
            } finally {
                sweeping.set(false);
            }
        }
    }

    public static long retryAfterSeconds(Instant now) {
        return Math.max(1, 60 - (now.getEpochSecond() % 60));
    }

    /** Package-visible: lets the sweep test assert bucket count without reflection. */
    int trackedKeys() {
        return buckets.size();
    }
}
