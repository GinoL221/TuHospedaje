package com.tuhospedaje.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "tuhospedaje.email-outbox", name = "enabled", havingValue = "true", matchIfMissing = true)
public class EmailOutboxConfiguration {
}
