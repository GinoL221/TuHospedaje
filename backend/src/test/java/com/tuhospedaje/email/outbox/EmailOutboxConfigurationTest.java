package com.tuhospedaje.email.outbox;

import com.tuhospedaje.configuration.EmailOutboxConfiguration;
import com.tuhospedaje.configuration.EmailOutboxProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

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

    @Configuration
    @EnableConfigurationProperties(EmailOutboxProperties.class)
    @Import(EmailOutboxConfiguration.class)
    static class EmailOutboxConfigurationTestConfig {
    }
}
