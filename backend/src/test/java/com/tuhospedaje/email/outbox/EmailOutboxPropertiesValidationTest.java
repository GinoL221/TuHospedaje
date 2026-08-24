package com.tuhospedaje.email.outbox;

import com.tuhospedaje.configuration.EmailOutboxProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class EmailOutboxPropertiesValidationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(EmailOutboxPropertiesTestConfig.class);

    @Test
    void loadsValidDefaults() {
        runner.withPropertyValues(
                        "tuhospedaje.email-outbox.enabled=true",
                        "tuhospedaje.email-outbox.poll-interval=PT30S",
                        "tuhospedaje.email-outbox.batch-size=20",
                        "tuhospedaje.email-outbox.lease-duration=PT5M",
                        "tuhospedaje.email-outbox.max-attempts=5",
                        "tuhospedaje.email-outbox.backoff[0]=PT1M",
                        "tuhospedaje.email-outbox.backoff[1]=PT5M",
                        "tuhospedaje.email-outbox.backoff[2]=PT15M",
                        "tuhospedaje.email-outbox.backoff[3]=PT1H",
                        "tuhospedaje.email-outbox.retention=P30D",
                        "tuhospedaje.email-outbox.cleanup-interval=P1D")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    EmailOutboxProperties props = context.getBean(EmailOutboxProperties.class);
                    assertThat(props.isEnabled()).isTrue();
                    assertThat(props.getPollInterval()).isEqualTo(Duration.ofSeconds(30));
                    assertThat(props.getBatchSize()).isEqualTo(20);
                    assertThat(props.getLeaseDuration()).isEqualTo(Duration.ofMinutes(5));
                    assertThat(props.getMaxAttempts()).isEqualTo(5);
                    assertThat(props.getBackoff()).containsExactly(
                            Duration.ofMinutes(1),
                            Duration.ofMinutes(5),
                            Duration.ofMinutes(15),
                            Duration.ofHours(1));
                    assertThat(props.getRetention()).isEqualTo(Duration.ofDays(30));
                    assertThat(props.getCleanupInterval()).isEqualTo(Duration.ofDays(1));
                });
    }

    @Test
    void rejectsZeroBatchSize() {
        runner.withPropertyValues(
                        "tuhospedaje.email-outbox.enabled=true",
                        "tuhospedaje.email-outbox.batch-size=0")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void rejectsNegativeMaxAttempts() {
        runner.withPropertyValues(
                        "tuhospedaje.email-outbox.enabled=true",
                        "tuhospedaje.email-outbox.max-attempts=-1")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void rejectsEmptyBackoff() {
        runner.withPropertyValues(
                        "tuhospedaje.email-outbox.enabled=true",
                        "tuhospedaje.email-outbox.backoff=")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void rejectsNonPositiveTimingControls() {
        runner.withPropertyValues(
                        "tuhospedaje.email-outbox.poll-interval=PT0S",
                        "tuhospedaje.email-outbox.lease-duration=PT0S",
                        "tuhospedaje.email-outbox.retention=PT0S",
                        "tuhospedaje.email-outbox.cleanup-interval=PT0S")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void rejectsBackoffThatCannotCoverConfiguredRetries() {
        runner.withPropertyValues(
                        "tuhospedaje.email-outbox.max-attempts=3",
                        "tuhospedaje.email-outbox.backoff[0]=PT1M")
                .run(context -> assertThat(context).hasFailed());
    }

    @Configuration
    @EnableConfigurationProperties(EmailOutboxProperties.class)
    static class EmailOutboxPropertiesTestConfig {
    }
}
