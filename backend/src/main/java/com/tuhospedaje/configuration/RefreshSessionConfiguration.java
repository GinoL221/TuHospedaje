package com.tuhospedaje.configuration;

import com.tuhospedaje.repository.RefreshTokenFamilyRepository;
import com.tuhospedaje.repository.RefreshTokenRepository;
import com.tuhospedaje.repository.SessionSecurityEventRepository;
import com.tuhospedaje.security.RefreshTokenHasher;
import com.tuhospedaje.service.RefreshSessionService;
import com.tuhospedaje.service.impl.RefreshSessionServiceImpl;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.function.Supplier;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "app.session.refresh.enabled", havingValue = "true")
@EnableConfigurationProperties(RefreshKeyRingProperties.class)
public class RefreshSessionConfiguration {

    @Bean
    RefreshTokenHasher refreshTokenHasher(RefreshKeyRingProperties keyRing, SecureRandom random) {
        return new RefreshTokenHasher(keyRing, random);
    }

    @Bean
    RefreshSessionService refreshSessionService(RefreshTokenFamilyRepository families,
                                                RefreshTokenRepository tokens,
                                                SessionSecurityEventRepository events,
                                                RefreshTokenHasher hasher,
                                                SessionProperties properties,
                                                Supplier<Clock> clock,
                                                EntityManager entityManager) {
        return new RefreshSessionServiceImpl(families, tokens, events, hasher, properties, clock, entityManager);
    }
}
