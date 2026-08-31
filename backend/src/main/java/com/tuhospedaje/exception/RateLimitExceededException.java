package com.tuhospedaje.exception;

/**
 * Thrown by {@code RefreshSessionServiceImpl.rotate()} when the resolved token family has
 * exceeded its per-family refresh rate ceiling (Design "Rate Limiting for POST
 * /api/auth/refresh"). Deliberately distinct from {@link
 * com.tuhospedaje.service.RefreshSessionService.Rejected}: the credential presented is
 * still live, so the caller must retry after {@link #getRetryAfterSeconds()} rather than
 * being treated as a dead/invalid session.
 *
 * <p>No {@code noRollbackFor} entry is added for this type on {@code rotate()} — unlike
 * {@code Rejected}, this exception rolls back its transaction, which is a no-op here
 * because Block B (the family check) runs before any write in that method.
 */
public class RateLimitExceededException extends RuntimeException {

    private final long retryAfterSeconds;

    public RateLimitExceededException(long retryAfterSeconds) {
        super("Rate limit exceeded");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
