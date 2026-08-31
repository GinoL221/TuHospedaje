package com.tuhospedaje.configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
@Validated
@ConfigurationProperties(prefix = "app.session")
public record SessionProperties(
        @NotNull @DurationMin(seconds = 1) Duration accessTokenLifetime,
        @NotNull @Valid RefreshProperties refresh,
        @NotNull @Valid CleanupProperties cleanup,
        @NotNull @Valid RateLimitProperties rateLimit
) {
    public record RefreshProperties(
            boolean enabled,
            @NotNull @DurationMin(seconds = 1) Duration absoluteLifetime,
            @NotNull @DurationMin(seconds = 1) Duration retryGrace
    ) {
    }

    public record CleanupProperties(@NotNull @DurationMin(seconds = 1) Duration interval, @Positive int batchSize) {
    }

    public record RateLimitProperties(
            boolean enabled,
            @Positive int refreshPerFamilyPerMinute,
            @Positive int refreshPerIpPerMinute) {
    }
}
