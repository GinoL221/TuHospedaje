package com.tuhospedaje.configuration;

import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public final class DevSeedFlywayGuard implements FlywayConfigurationCustomizer {

    private static final String DEVELOPMENT_PROFILE = "dev";
    private static final String PRODUCTION_MIGRATION_LOCATION = "classpath:db/migration";
    private static final String DEVELOPMENT_MIGRATION_LOCATION = "classpath:db/dev";
    private static final Set<String> PRODUCTION_LOCATIONS = Set.of(PRODUCTION_MIGRATION_LOCATION);
    private static final Set<String> DEVELOPMENT_LOCATIONS =
            Set.of(PRODUCTION_MIGRATION_LOCATION, DEVELOPMENT_MIGRATION_LOCATION);

    private final Environment environment;

    public DevSeedFlywayGuard(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void customize(FluentConfiguration configuration) {
        Set<String> configuredLocations = Arrays.stream(configuration.getLocations())
                .map(location -> location.getDescriptor())
                .collect(Collectors.toUnmodifiableSet());
        Set<String> activeProfiles = Set.of(environment.getActiveProfiles());
        Set<String> allowedLocations = activeProfiles.equals(Set.of(DEVELOPMENT_PROFILE))
                ? DEVELOPMENT_LOCATIONS
                : PRODUCTION_LOCATIONS;
        if (!PRODUCTION_LOCATIONS.stream().allMatch(configuredLocations::contains)
                || !allowedLocations.containsAll(configuredLocations)) {
            throw new IllegalStateException("Flyway locations must use the exact application migration allowlist");
        }
    }
}
