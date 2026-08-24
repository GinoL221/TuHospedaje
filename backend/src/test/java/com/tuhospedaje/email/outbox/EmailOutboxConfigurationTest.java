package com.tuhospedaje.email.outbox;

import com.tuhospedaje.configuration.EmailOutboxConfiguration;
import com.tuhospedaje.configuration.EmailOutboxProperties;
import com.tuhospedaje.service.impl.EmailOutboxScheduler;
import com.tuhospedaje.service.impl.EmailOutboxTransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class EmailOutboxConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(EmailOutboxConfigurationTestConfig.class);

    @Test
    void configurationLoadsWhenEnabled() {
        runner.withPropertyValues("tuhospedaje.email-outbox.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBeanDefinitionNames())
                            .anyMatch(name -> name.contains("EmailOutboxConfiguration"));
                });
    }

    @Test
    void configurationDoesNotLoadWhenDisabled() {
        runner.withPropertyValues("tuhospedaje.email-outbox.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(EmailOutboxConfiguration.class));
    }

    @Test
    void configurationDoesNotLoadWhenDispatchControlIsUnset() {
        runner.run(context -> assertThat(context).doesNotHaveBean(EmailOutboxConfiguration.class));
    }

    @Test
    void enabledCleanupSchedulingDoesNotRequireAnUnavailableSmtpDispatcher() {
        new ApplicationContextRunner()
                .withUserConfiguration(EmailOutboxSchedulerWithoutTransportTestConfig.class)
                .withPropertyValues(
                        "tuhospedaje.email-outbox.enabled=true",
                        "tuhospedaje.email-outbox.poll-interval=PT30S",
                        "tuhospedaje.email-outbox.cleanup-interval=P1D")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(EmailOutboxScheduler.class);
                });
    }

    @Configuration
    @EnableConfigurationProperties(EmailOutboxProperties.class)
    @Import(EmailOutboxConfiguration.class)
    static class EmailOutboxConfigurationTestConfig {
    }

    @Configuration
    @EnableConfigurationProperties(EmailOutboxProperties.class)
    @Import({EmailOutboxConfiguration.class, EmailOutboxScheduler.class})
    static class EmailOutboxSchedulerWithoutTransportTestConfig {

        @Bean
        EmailOutboxTransactionService emailOutboxTransactionService() {
            return mock(EmailOutboxTransactionService.class);
        }
    }
}
