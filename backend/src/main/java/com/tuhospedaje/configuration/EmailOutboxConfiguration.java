package com.tuhospedaje.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "tuhospedaje.email-outbox", name = "enabled", havingValue = "true")
public class EmailOutboxConfiguration {
}
