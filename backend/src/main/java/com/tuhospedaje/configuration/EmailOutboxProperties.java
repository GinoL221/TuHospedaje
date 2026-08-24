package com.tuhospedaje.configuration;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.AssertTrue;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Validated
@Getter
@Setter
@ConfigurationProperties(prefix = "tuhospedaje.email-outbox")
public class EmailOutboxProperties {

    private boolean enabled = false;

    @NotNull
    private Duration pollInterval = Duration.ofSeconds(30);

    @Min(1)
    private int batchSize = 20;

    @NotNull
    private Duration leaseDuration = Duration.ofMinutes(5);

    @Min(1)
    private int maxAttempts = 5;

    @NotEmpty
    private List<@NotNull Duration> backoff = new ArrayList<>(List.of(
            Duration.ofMinutes(1),
            Duration.ofMinutes(5),
            Duration.ofMinutes(15),
            Duration.ofHours(1)));

    @NotNull
    private Duration retention = Duration.ofDays(30);

    @NotNull
    private Duration cleanupInterval = Duration.ofDays(1);

    @AssertTrue(message = "email outbox timing and retry controls must be positive and bounded")
    public boolean hasSafeDispatchControls() {
        return isBetween(pollInterval, Duration.ofSeconds(1), Duration.ofHours(1))
                && batchSize <= 100
                && isBetween(leaseDuration, Duration.ofSeconds(30), Duration.ofMinutes(30))
                && maxAttempts <= 20
                && backoff != null
                && backoff.size() >= Math.max(0, maxAttempts - 1)
                && backoff.stream().allMatch(value -> isBetween(value, Duration.ofSeconds(1), Duration.ofDays(1)))
                && isBetween(retention, Duration.ofDays(1), Duration.ofDays(365))
                && isBetween(cleanupInterval, Duration.ofHours(1), Duration.ofDays(30));
    }

    private boolean isBetween(Duration value, Duration minimum, Duration maximum) {
        return value != null && value.compareTo(minimum) >= 0 && value.compareTo(maximum) <= 0;
    }
}
