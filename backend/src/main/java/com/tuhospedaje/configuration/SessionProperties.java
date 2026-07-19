package com.tuhospedaje.configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Validated
@ConfigurationProperties(prefix = "app.session")
public record SessionProperties(
        @NotNull @DurationMin(seconds = 1) Duration accessTokenLifetime,
        @NotNull @Valid RefreshProperties refresh,
        @NotNull @Valid KeyRingProperties keyRing,
        @NotNull @Valid CleanupProperties cleanup,
        @NotNull @Valid RateLimitProperties rateLimit
) {
    public record RefreshProperties(
            boolean enabled,
            @NotNull @DurationMin(seconds = 1) Duration absoluteLifetime,
            @NotNull @DurationMin(seconds = 1) Duration retryGrace
    ) {
    }

    public record KeyRingProperties(
            @NotBlank String activeKeyId,
            @NotEmpty @Valid List<KeyEntry> keyEntries
    ) {
        public Map<String, String> keys() {
            return keyEntries.stream().collect(Collectors.toUnmodifiableMap(KeyEntry::id, KeyEntry::secret));
        }
    }

    public record KeyEntry(@NotBlank String id, @NotBlank String secret) {
    }

    public record CleanupProperties(@NotNull @DurationMin(seconds = 1) Duration interval, @Positive int batchSize) {
    }

    public record RateLimitProperties(@Positive int refreshPerFamilyPerMinute, @Positive int refreshPerIpPerMinute) {
    }
}
