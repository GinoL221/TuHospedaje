package com.tuhospedaje.configuration;

import com.tuhospedaje.repository.RefreshTokenFamilyRepository;
import com.tuhospedaje.repository.RefreshTokenRepository;
import com.tuhospedaje.repository.SessionSecurityEventRepository;
import com.tuhospedaje.repository.UserRepository;
import com.tuhospedaje.security.RefreshTokenHasher;
import com.tuhospedaje.service.RefreshSessionService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RefreshSessionConfigurationTest {

    private static final String[] BASE_PROPERTIES = {
            "app.session.access-token-lifetime=PT15M",
            "app.session.refresh.absolute-lifetime=P30D",
            "app.session.refresh.retry-grace=PT5S",
            "app.session.cleanup.interval=P1D",
            "app.session.cleanup.batch-size=100",
            "app.session.rate-limit.refresh-per-family-per-minute=10",
            "app.session.rate-limit.refresh-per-ip-per-minute=60"
    };

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class)
            .withBean(UserRepository.class, () -> mock(UserRepository.class))
            .withBean(AuthenticationConfiguration.class, RefreshSessionConfigurationTest::authenticationConfiguration)
            .withBean(RefreshTokenFamilyRepository.class, () -> mock(RefreshTokenFamilyRepository.class))
            .withBean(RefreshTokenRepository.class, () -> mock(RefreshTokenRepository.class))
            .withBean(SessionSecurityEventRepository.class, () -> mock(SessionSecurityEventRepository.class))
            .withBean(EntityManager.class, () -> mock(EntityManager.class))
            .withPropertyValues(BASE_PROPERTIES);

    @Test
    void startsWithoutRefreshKeysAndDoesNotExposeRefreshBeansWhenDisabled() {
        contextRunner.withPropertyValues("app.session.refresh.enabled=false").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(RefreshTokenHasher.class);
            assertThat(context).doesNotHaveBean(RefreshSessionService.class);
        });
    }

    @Test
    void rejectsMissingRefreshKeysWhenEnabled() {
        contextRunner.withPropertyValues("app.session.refresh.enabled=true")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void rejectsBlankRefreshKeysWhenEnabled() {
        contextRunner.withPropertyValues(
                        "app.session.refresh.enabled=true",
                        "app.session.key-ring.active-key-id=",
                        "app.session.key-ring.key-entries[0].id=",
                        "app.session.key-ring.key-entries[0].secret=")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void rejectsKeyRingWithoutItsActiveKeyWhenEnabled() {
        contextRunner.withPropertyValues(
                        "app.session.refresh.enabled=true",
                        "app.session.key-ring.active-key-id=active",
                        "app.session.key-ring.key-entries[0].id=retired",
                        "app.session.key-ring.key-entries[0].secret=retired-test-secret")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void exposesRefreshBeansForAValidEnabledKeyRing() {
        contextRunner.withPropertyValues(
                        "app.session.refresh.enabled=true",
                        "app.session.key-ring.active-key-id=active",
                        "app.session.key-ring.key-entries[0].id=active",
                        "app.session.key-ring.key-entries[0].secret=active-test-secret")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(RefreshTokenHasher.class);
                    assertThat(context).hasSingleBean(RefreshSessionService.class);
                });
    }

    private static AuthenticationConfiguration authenticationConfiguration() {
        AuthenticationConfiguration configuration = mock(AuthenticationConfiguration.class);
        try {
            when(configuration.getAuthenticationManager()).thenReturn(mock(AuthenticationManager.class));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
        return configuration;
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(SessionProperties.class)
    @Import({ApplicationConfig.class, RefreshSessionConfiguration.class})
    static class TestConfiguration {
    }
}
