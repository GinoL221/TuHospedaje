package com.tuhospedaje.configuration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.mock.env.MockEnvironment;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DevSeedFlywayGuardTest {

    private static final String GUARD_MESSAGE =
            "Flyway locations must use the exact application migration allowlist";

    @Test
    void permitsDevelopmentSeedOnlyWithTheDevelopmentProfile() {
        DevSeedFlywayGuard guard = guardWithProfiles("dev");

        assertThatCode(() -> guard.customize(developmentSeedConfiguration())).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @MethodSource("rejectedLocationConfigurations")
    void rejectsLocationsOutsideTheProfileAllowlist(String[] profiles, String[] locations) {
        assertThatThrownBy(() -> guardWithProfiles(profiles).customize(configurationWithLocations(locations)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(GUARD_MESSAGE);
    }

    @Test
    void ignoresOrdinarySchemaMigrationsForEveryProfile() {
        DevSeedFlywayGuard guard = guardWithProfiles("prod");

        assertThatCode(() -> guard.customize(Flyway.configure().locations("classpath:db/migration")))
                .doesNotThrowAnyException();
    }

    private static Stream<Arguments> rejectedLocationConfigurations() {
        return Stream.of(
                Arguments.of(new String[]{}, new String[]{"classpath:db/migration", "classpath:db/dev"}),
                Arguments.of(new String[]{"prod"}, new String[]{"classpath:db/migration", "classpath:db/dev"}),
                Arguments.of(new String[]{"dev", "prod"}, new String[]{"classpath:db/migration", "classpath:db/dev"}),
                Arguments.of(new String[]{"prod"}, new String[]{"classpath:db"}),
                Arguments.of(new String[]{}, new String[]{"classpath:db/**"}),
                Arguments.of(new String[]{"dev", "prod"}, new String[]{"classpath:db/migration", "filesystem:migrations"}),
                Arguments.of(new String[]{"dev"}, new String[]{"classpath:db"}),
                Arguments.of(new String[]{"dev"}, new String[]{"classpath:db/migration", "classpath:db/*"})
        );
    }

    private DevSeedFlywayGuard guardWithProfiles(String... profiles) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(profiles);
        return new DevSeedFlywayGuard(environment);
    }

    private org.flywaydb.core.api.configuration.FluentConfiguration developmentSeedConfiguration() {
        return configurationWithLocations("classpath:db/migration", "classpath:db/dev");
    }

    private org.flywaydb.core.api.configuration.FluentConfiguration configurationWithLocations(String... locations) {
        return Flyway.configure().locations(locations);
    }
}
