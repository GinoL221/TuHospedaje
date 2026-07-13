package com.tuhospedaje;

import com.tuhospedaje.configuration.TestcontainersConfiguration;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.MapPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.MariaDBContainer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class DatabaseMigrationIntegrationTest {

    private static final String WILDCARD_HOST = "%";
    private static final String TEMPORARY_DDL_DML_PRIVILEGES =
            "SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, DROP, INDEX, REFERENCES";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MariaDBContainer<?> mariadbContainer;

    @Test
    void migratesAnEmptyDatabaseThroughFlywayAndDoesNotLoadDemoData() {
        Integer historyTableCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE() AND table_name = 'flyway_schema_history'
                """, Integer.class);

        Integer userCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        Integer lodgingCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM lodgings", Integer.class);

        assertThat(historyTableCount).isEqualTo(1);
        assertThat(userCount).isZero();
        assertThat(lodgingCount).isZero();
    }

    @Test
    void startsTheActualDefaultProfileWithoutDemoData() {
        assertActualProfileStartupHasNoDemoData();
    }

    @Test
    void startsTheActualProductionProfileWithoutDemoData() {
        assertActualProfileStartupHasNoDemoData("prod");
    }

    @Test
    void migratesAndValidatesV1InAnIndependentSecondMariaDbContainer() {
        try (MariaDBContainer<?> independentContainer = new MariaDBContainer<>("mariadb:10.11")
                .withDatabaseName("independent_v1_probe")
                .withUsername("test")
                .withPassword("test")) {
            independentContainer.start();

            Flyway flyway = Flyway.configure()
                    .dataSource(
                            independentContainer.getJdbcUrl(),
                            independentContainer.getUsername(),
                            independentContainer.getPassword())
                    .locations("classpath:db/migration")
                    .load();

            assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);

            try (ConfigurableApplicationContext context = new SpringApplicationBuilder(BackendApplication.class)
                    .properties("spring.main.web-application-type=none")
                    .initializers(applicationContext -> applicationContext.getEnvironment().getPropertySources().addFirst(
                            new MapPropertySource("independent-v1-testcontainer", Map.of(
                                    "spring.datasource.url", independentContainer.getJdbcUrl(),
                                    "spring.datasource.username", independentContainer.getUsername(),
                                    "spring.datasource.password", independentContainer.getPassword()
                            ))))
                    .run()) {
                assertThat(context.isActive()).isTrue();
            }
        }
    }

    @Test
    void rejectsAnAppliedMigrationWhoseChecksumChanges(@TempDir Path migrationsDirectory) throws IOException {
        Path migration = migrationsDirectory.resolve("V1__checksum_probe.sql");
        Files.writeString(migration, "CREATE TABLE checksum_probe (id INT PRIMARY KEY);\n");

        ProbeDatabase probe = createProbeDatabase("checksum_probe");
        try {
            Flyway firstRun = Flyway.configure()
                    .dataSource(probe.jdbcUrl(), probe.username(), probe.password())
                    .locations("filesystem:" + migrationsDirectory)
                    .table("checksum_probe_history")
                    .load();
            firstRun.migrate();

            Files.writeString(migration, "CREATE TABLE checksum_probe (id BIGINT PRIMARY KEY);\n");

            assertThatThrownBy(() -> firstRun.validate())
                    .hasMessageContaining("checksum");
        } finally {
            dropProbeDatabase(probe);
        }
    }

    @Test
    void grantsEachProbeAccountAccessOnlyToItsTemporarySchema() {
        ProbeDatabase probe = createProbeDatabase("privilege_probe");
        try {
            assertProbeAccountHasOnlyItsSchemaPrivileges(probe);
        } finally {
            dropProbeDatabase(probe);
        }
    }

    @Test
    void rejectsStartupWhenTheMigratedSchemaDriftsFromHibernateMappings() {
        ProbeDatabase probe = createProbeDatabase("mapping_drift_probe");
        try {
            Flyway.configure()
                    .dataSource(probe.jdbcUrl(), probe.username(), probe.password())
                    .locations("classpath:db/migration")
                    .load()
                    .migrate();
            executeAsContainerRoot("ALTER TABLE `" + probe.schema() + "`.`users` DROP COLUMN `first_name`");

            assertThatThrownBy(() -> new SpringApplicationBuilder(BackendApplication.class)
                    .properties("spring.main.web-application-type=none")
                    .run(
                            "--spring.datasource.url=" + probe.jdbcUrl(),
                            "--spring.datasource.username=" + probe.username(),
                            "--spring.datasource.password=" + probe.password(),
                            "--spring.flyway.user=" + probe.username(),
                            "--spring.flyway.password=" + probe.password()
                    ))
                    .hasStackTraceContaining("first_name");
        } finally {
            dropProbeDatabase(probe);
        }
    }

    private void assertActualProfileStartupHasNoDemoData(String... profiles) {
        String databaseName = profiles.length == 0 ? "default_profile_probe" : "prod_profile_probe";
        String probeSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String probeUsername = "profile_" + probeSuffix;
        String probePassword = UUID.randomUUID().toString();
        try (MariaDBContainer<?> profileContainer = new MariaDBContainer<>("mariadb:10.11")
                .withDatabaseName(databaseName)
                .withUsername(probeUsername)
                .withPassword(probePassword)) {
            profileContainer.start();

            String mainResourcesDirectory = Path.of("src/main/resources").toAbsolutePath().toString();
            try (ConfigurableApplicationContext context = new SpringApplicationBuilder(BackendApplication.class)
                    .profiles(profiles)
                    .properties(
                            "spring.main.web-application-type=none",
                            "spring.config.location=optional:file:" + mainResourcesDirectory + "/"
                    )
                    .initializers(applicationContext -> applicationContext.getEnvironment().getPropertySources().addFirst(
                            new MapPropertySource("actual-profile-testcontainer", Map.of(
                                    "app.jwt.secret", "dGVzdHNlY3JldDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDEyMzQ1Njc4OTA=",
                                    "app.cors.allowed-origins", "http://localhost:5173"
                            ))))
                    .run(
                            "--spring.datasource.url=" + profileContainer.getJdbcUrl(),
                            "--spring.datasource.username=" + probeUsername,
                            "--spring.datasource.password=" + probePassword,
                            "--spring.flyway.user=" + probeUsername,
                            "--spring.flyway.password=" + probePassword
                    )) {
                JdbcTemplate profileJdbcTemplate = context.getBean(JdbcTemplate.class);

                assertThat(context.getEnvironment().getActiveProfiles()).containsExactly(profiles);
                assertThat(context.getEnvironment().getProperty("spring.flyway.locations"))
                        .isEqualTo("classpath:db/migration");
                assertThat(context.getEnvironment().getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
                assertThat(context.getEnvironment().getProperty("spring.sql.init.mode")).isEqualTo("never");
                assertThat(profileJdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class)).isZero();
                assertThat(profileJdbcTemplate.queryForObject("SELECT COUNT(*) FROM lodgings", Integer.class)).isZero();
            }
        }
    }

    private ProbeDatabase createProbeDatabase(String schemaPrefix) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String schema = schemaPrefix + "_" + suffix;
        String username = "probe_" + suffix;
        String password = UUID.randomUUID().toString();
        ProbeDatabase probe = new ProbeDatabase(schema, username, password, jdbcUrlFor(schema));
        try {
            executeAsContainerRoot("CREATE DATABASE `" + schema + "`");
            executeAsContainerRoot("CREATE USER '" + username + "'@'" + WILDCARD_HOST
                    + "' IDENTIFIED BY '" + password + "'");
            executeAsContainerRoot("GRANT " + TEMPORARY_DDL_DML_PRIVILEGES + " ON `" + schema + "`.* TO '"
                    + username + "'@'" + WILDCARD_HOST + "'");
            return probe;
        } catch (RuntimeException exception) {
            dropProbeDatabase(probe);
            throw exception;
        }
    }

    private void dropProbeDatabase(ProbeDatabase probe) {
        executeAsContainerRoot("DROP DATABASE IF EXISTS `" + probe.schema() + "`");
        executeAsContainerRoot("DROP USER IF EXISTS '" + probe.username() + "'@'" + WILDCARD_HOST + "'");
    }

    private String jdbcUrlFor(String schema) {
        String containerUrl = mariadbContainer.getJdbcUrl();
        int databaseStart = containerUrl.lastIndexOf('/') + 1;
        int parametersStart = containerUrl.indexOf('?', databaseStart);
        String parameters = parametersStart < 0 ? "" : containerUrl.substring(parametersStart);
        return containerUrl.substring(0, databaseStart) + schema + parameters;
    }

    private void assertProbeAccountHasOnlyItsSchemaPrivileges(ProbeDatabase probe) {
        try (Connection connection = DriverManager.getConnection(probe.jdbcUrl(), probe.username(), probe.password());
             var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE privilege_check (id INT PRIMARY KEY)");
            statement.execute("DROP TABLE privilege_check");
        } catch (SQLException exception) {
            throw new IllegalStateException("Probe account cannot use its dedicated schema", exception);
        }

        try (Connection connection = DriverManager.getConnection(
                mariadbContainer.getJdbcUrl(),
                "root",
                mariadbContainer.getEnvMap().get("MYSQL_ROOT_PASSWORD")
        ); var statement = connection.prepareStatement("""
                SELECT COUNT(*)
                FROM information_schema.schema_privileges
                WHERE grantee = ?
                  AND table_schema <> ?
                """)) {
            statement.setString(1, "'" + probe.username() + "'@'%'");
            statement.setString(2, probe.schema());
            try (var resultSet = statement.executeQuery()) {
                resultSet.next();
                assertThat(resultSet.getInt(1)).isZero();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to inspect temporary test-account privileges", exception);
        }
    }

    private void executeAsContainerRoot(String sql) {
        try (Connection connection = DriverManager.getConnection(
                mariadbContainer.getJdbcUrl(),
                "root",
                mariadbContainer.getEnvMap().get("MYSQL_ROOT_PASSWORD")
        ); var statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to provision the checksum test schema", exception);
        }
    }

    private record ProbeDatabase(String schema, String username, String password, String jdbcUrl) {
    }
}
