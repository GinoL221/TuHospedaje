package com.tuhospedaje.configuration;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Config for the login/register attempt ceiling (Auth Rate Limiting spec/design).
 *
 * <p>Four independent thresholds instead of one shared limit: legitimate login retries
 * (typos) and one-shot registration have very different honest traffic profiles — see
 * design "Decision: Four thresholds (login/register x IP/email), not one shared limit".
 * Naming mirrors the existing per-dimension shape of
 * {@link SessionProperties.RateLimitProperties} ({@code refreshPerFamilyPerMinute} /
 * {@code refreshPerIpPerMinute}).
 *
 * <p>{@code enabled} is read by {@link AuthRateLimitFilter#shouldNotFilter} rather than
 * gating bean creation with {@code @ConditionalOnProperty} — see design "Decision: Kill
 * switch checked in shouldNotFilter(), not @ConditionalOnProperty" for why a conditional
 * bean does not fit here.
 */
@Validated
@ConfigurationProperties(prefix = "app.auth.rate-limit")
public record AuthRateLimitProperties(
        boolean enabled,
        @Positive int loginPerIpPerMinute,
        @Positive int loginPerEmailPerMinute,
        @Positive int registerPerIpPerMinute,
        @Positive int registerPerEmailPerMinute
) {
}
