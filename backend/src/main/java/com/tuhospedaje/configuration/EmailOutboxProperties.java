package com.tuhospedaje.configuration;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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

    private boolean enabled = true;

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
}
