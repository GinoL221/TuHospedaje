package com.tuhospedaje.configuration;

import jakarta.validation.ClockProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class TimeConfiguration {

    @Bean
    Clock businessClock(@Value("${app.business-time-zone}") String businessTimeZone) {
        return Clock.system(ZoneId.of(businessTimeZone));
    }

    @Bean
    ClockProvider clockProvider(Clock businessClock) {
        return () -> businessClock;
    }
}
