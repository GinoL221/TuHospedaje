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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.testcontainers.containers.MariaDBContainer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class DatabaseMigrationIntegrationTest {

    private static final String WILDCARD_HOST = "%";
    private static final String TEMPORARY_DDL_DML_PRIVILEGES =
            "SELECT, INSERT, UPDATE, DELETE, CREATE, CREATE TEMPORARY TABLES, ALTER, DROP, INDEX, REFERENCES";
    private static final String DEV_ADMIN_PASSWORD = "local-dev-admin-password";
    private final String validDevAdminPasswordHash = new BCryptPasswordEncoder(14).encode(DEV_ADMIN_PASSWORD);

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
    void keepsDevelopmentAndDefaultProfileStartupsSchemaOnly() {
        assertActualDevelopmentProfileStartupHasNoDemoData();
        assertActualProfileStartupHasNoDemoData();
    }

    @Test
    void loadsCanonicalDemoDataOnlyWithExplicitSeedOptIn() {
        ProbeDatabase probe = createProbeDatabase("explicit_dev_seed_probe");
        String mainResourcesDirectory = Path.of("src/main/resources").toAbsolutePath().toString();
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(BackendApplication.class)
                .profiles("dev")
                .properties(
                        "spring.main.web-application-type=none",
                        "spring.config.location=optional:file:" + mainResourcesDirectory + "/"
                )
                .run(
                        "--spring.datasource.url=" + probe.jdbcUrl(),
                        "--spring.datasource.username=" + probe.username(),
                        "--spring.datasource.password=" + probe.password(),
                        "--spring.flyway.locations=classpath:db/migration,classpath:db/dev",
                        "--spring.flyway.placeholders.dev_admin_password_hash=" + validDevAdminPasswordHash()
                )) {
            JdbcTemplate probeJdbcTemplate = context.getBean(JdbcTemplate.class);
            assertThat(context.getEnvironment().getActiveProfiles()).containsExactly("dev");
assertThat(probeJdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM flyway_schema_history", Integer.class)).isEqualTo(3);
            assertThat(probeJdbcTemplate.queryForObject("SELECT COUNT(*) FROM categories", Integer.class)).isEqualTo(6);
            assertThat(probeJdbcTemplate.queryForObject("SELECT COUNT(*) FROM features", Integer.class)).isEqualTo(8);
            assertThat(probeJdbcTemplate.queryForObject("SELECT COUNT(*) FROM policies", Integer.class)).isEqualTo(6);
            assertThat(probeJdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class)).isEqualTo(1);
            assertThat(probeJdbcTemplate.queryForObject("SELECT COUNT(*) FROM lodgings", Integer.class)).isEqualTo(38);
            assertThat(probeJdbcTemplate.queryForObject("SELECT COUNT(*) FROM lodging_images", Integer.class)).isEqualTo(190);
            assertThat(probeJdbcTemplate.queryForObject("SELECT COUNT(*) FROM lodging_features", Integer.class)).isEqualTo(154);
            assertThat(probeJdbcTemplate.queryForObject("SELECT COUNT(*) FROM lodging_policies", Integer.class)).isEqualTo(161);
            assertThat(probeJdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM categories WHERE id = 6 AND name = 'Glamping'", Integer.class)).isEqualTo(1);
            assertThat(probeJdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM features WHERE id = 8 AND name = 'Cocina equipada'", Integer.class)).isEqualTo(1);
            assertThat(probeJdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM policies WHERE id = 6 AND name = 'Fiestas'", Integer.class)).isEqualTo(1);
            assertThat(probeJdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM users WHERE id = 1 AND email = 'admin@tuhospedaje.com' AND role = 'ADMIN'",
                    Integer.class)).isEqualTo(1);
            assertThat(probeJdbcTemplate.queryForObject(
                    "SELECT password FROM users WHERE id = 1", String.class)).isEqualTo(validDevAdminPasswordHash());
            assertThat(new BCryptPasswordEncoder().matches(
                    validDevAdminPassword(),
                    probeJdbcTemplate.queryForObject("SELECT password FROM users WHERE id = 1", String.class)))
                    .isTrue();
            assertCanonicalSeedManifest(probeJdbcTemplate);
        } finally {
            dropProbeDatabase(probe);
        }
    }

    @Test
    void rejectsExplicitSeedOnNonDevelopmentAndMixedProductionDatabasesBeforeWrites() {
        for (String databaseName : new String[]{"customer_database", "dev_prod_seed_probe"}) {
            ProbeDatabase probe = createProbeDatabase(databaseName);
            try {
                assertThatThrownBy(() -> configuredDevSeedFlyway(probe, validDevAdminPasswordHash()).migrate())
                        .hasMessageContaining("chk_dev_seed_database_name");
                assertSeedOwnedTablesAreEmpty(jdbcTemplateFor(probe));
            } finally {
                dropProbeDatabase(probe);
            }
        }
    }

    @Test
    void rejectsWeakAndMalformedBcryptHashesBeforeWrites() {
        for (String invalidHash : new String[]{
                "$2a$09$AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                "$2x$14$AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
        }) {
            ProbeDatabase probe = createProbeDatabase("dev_invalid_hash_probe");
            try {
                assertThatThrownBy(() -> configuredDevSeedFlyway(probe, invalidHash).migrate())
                        .hasMessageContaining("chk_dev_seed_bcrypt_hash");
                assertSeedOwnedTablesAreEmpty(jdbcTemplateFor(probe));
            } finally {
                dropProbeDatabase(probe);
            }
        }
    }

    @Test
    void rejectsExplicitSeedOptInWithoutAnAdminPasswordHash() {
        ProbeDatabase probe = createProbeDatabase("missing_dev_seed_hash_probe");
        try {
            Flyway flyway = configuredDevSeedFlyway(probe, "");

            assertThatThrownBy(flyway::migrate)
                    .hasMessageContaining("chk_dev_seed_bcrypt_hash");

            JdbcTemplate probeJdbcTemplate = jdbcTemplateFor(probe);
            assertThat(probeJdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class)).isZero();
            assertThat(probeJdbcTemplate.queryForObject("SELECT COUNT(*) FROM lodgings", Integer.class)).isZero();
        } finally {
            dropProbeDatabase(probe);
        }
    }

    @Test
    void rejectsSeedCollisionDuringPreflightBeforePersistentWrites() {
        ProbeDatabase probe = createProbeDatabase("dev_seed_collision_probe");
        try {
            Flyway.configure()
                    .dataSource(probe.jdbcUrl(), probe.username(), probe.password())
                    .locations("classpath:db/migration")
                    .load()
                    .migrate();
            JdbcTemplate probeJdbcTemplate = jdbcTemplateFor(probe);
            probeJdbcTemplate.update("""
                    INSERT INTO lodgings
                        (id, name, description, address, city, country, phone_number, email,
                         category_id, price_per_night, max_guests)
                    VALUES (1, 'Existing lodging', 'Not demo data', 'Existing address', 'Existing city',
                            'Argentina', '+54000000000', 'existing@example.com', NULL, 99.00, 2)
                    """);

            assertThatThrownBy(() -> configuredDevSeedFlyway(probe, validDevAdminPasswordHash()).migrate())
                    .hasMessageContaining("chk_dev_seed_tables_empty");

            assertThat(probeJdbcTemplate.queryForObject("SELECT COUNT(*) FROM categories", Integer.class)).isZero();
            assertThat(probeJdbcTemplate.queryForObject("SELECT COUNT(*) FROM features", Integer.class)).isZero();
            assertThat(probeJdbcTemplate.queryForObject("SELECT COUNT(*) FROM policies", Integer.class)).isZero();
            assertThat(probeJdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class)).isZero();
            assertThat(probeJdbcTemplate.queryForObject("SELECT COUNT(*) FROM lodgings", Integer.class)).isEqualTo(1);
            assertThat(probeJdbcTemplate.queryForObject("SELECT COUNT(*) FROM lodging_images", Integer.class)).isZero();
            assertThat(probeJdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM lodging_images WHERE lodging_id = 1", Integer.class)).isZero();
            assertThat(probeJdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM lodging_features WHERE lodging_id = 1", Integer.class)).isZero();
            assertThat(probeJdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM lodging_policies WHERE lodging_id = 1", Integer.class)).isZero();
            assertThat(probeJdbcTemplate.queryForObject(
                    "SELECT name FROM lodgings WHERE id = 1", String.class)).isEqualTo("Existing lodging");

        } finally {
            dropProbeDatabase(probe);
        }
    }

    @Test
    void migratesAndValidatesTheV1ToV2ChainInAnIndependentSecondMariaDbContainer() {
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

            assertThat(flyway.migrate().migrationsExecuted).isEqualTo(2);

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
    void retriesV2AfterEnabledColumnWasCommittedByPartialDdl() {
        ProbeDatabase probe = createProbeDatabase("partial_v2_probe");
        try {
            Flyway.configure()
                    .dataSource(probe.jdbcUrl(), probe.username(), probe.password())
                    .locations("classpath:db/migration")
                    .target("1")
                    .load()
                    .migrate();
            JdbcTemplate template = jdbcTemplateFor(probe);
            template.execute("ALTER TABLE users ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE");

            int migrationsExecuted = Flyway.configure()
                    .dataSource(probe.jdbcUrl(), probe.username(), probe.password())
                    .locations("classpath:db/migration")
                    .load()
                    .migrate().migrationsExecuted;

            assertThat(migrationsExecuted).isEqualTo(1);
            assertThat(template.queryForObject("""
                    SELECT COUNT(*) FROM information_schema.columns
                    WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'enabled'
                      AND data_type = 'tinyint' AND is_nullable = 'NO' AND column_default = '1'
                    """, Integer.class)).isEqualTo(1);
            assertThat(template.queryForObject("""
                    SELECT COUNT(*) FROM information_schema.tables
                    WHERE table_schema = DATABASE()
                      AND table_name IN ('refresh_token_families', 'refresh_tokens', 'session_security_events')
                    """, Integer.class)).isEqualTo(3);
            assertThat(template.queryForObject(
                    "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '2' AND success = 1",
                    Integer.class)).isEqualTo(1);
        } finally {
            dropProbeDatabase(probe);
        }
    }

    @Test
    void retriesV2AfterEachRefreshSessionTableWasCommittedByPartialDdl() {
        String[] v2Statements = loadV2Statements();

        for (int completedStatementCount : List.of(2, 3, 4)) {
            ProbeDatabase probe = createProbeDatabase("partial_v2_table_probe");
            try {
                Flyway.configure()
                        .dataSource(probe.jdbcUrl(), probe.username(), probe.password())
                        .locations("classpath:db/migration")
                        .target("1")
                        .load()
                        .migrate();

                JdbcTemplate template = jdbcTemplateFor(probe);
                for (int statementIndex = 0; statementIndex < completedStatementCount; statementIndex++) {
                    template.execute(v2Statements[statementIndex]);
                }

                int migrationsExecuted = Flyway.configure()
                        .dataSource(probe.jdbcUrl(), probe.username(), probe.password())
                        .locations("classpath:db/migration")
                        .load()
                        .migrate().migrationsExecuted;

                assertThat(migrationsExecuted).isEqualTo(1);
                assertThat(template.queryForObject("""
                        SELECT COUNT(*) FROM information_schema.tables
                        WHERE table_schema = DATABASE()
                          AND table_name IN ('refresh_token_families', 'refresh_tokens', 'session_security_events')
                        """, Integer.class)).isEqualTo(3);
                assertThat(template.queryForObject(
                        "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '2' AND success = 1",
                        Integer.class)).isEqualTo(1);
            } finally {
                dropProbeDatabase(probe);
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
                                    "app.cors.allowed-origins", "http://localhost:5173",
                                    // PR1/WU2 flips app.session.refresh.enabled=true in application.properties,
                                    // so the actual default/prod profile now needs an environment-backed key
                                    // ring too (mirrors SESSION_ACTIVE_KEY_ID/SESSION_REFRESH_KEY, which have no
                                    // fallback in production — see .env.example), exactly like app.jwt.secret above.
                                    "app.session.key-ring.active-key-id", "test-actual-profile-rt1",
                                    "app.session.key-ring.key-entries[0].id", "test-actual-profile-rt1",
                                    "app.session.key-ring.key-entries[0].secret", "test-actual-profile-refresh-key-not-for-production"
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

    private void assertActualDevelopmentProfileStartupHasNoDemoData() {
        String probeSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String probeUsername = "dev_profile_" + probeSuffix;
        String probePassword = UUID.randomUUID().toString();
        try (MariaDBContainer<?> profileContainer = new MariaDBContainer<>("mariadb:10.11")
                .withDatabaseName("dev_profile_probe")
                .withUsername(probeUsername)
                .withPassword(probePassword)) {
            profileContainer.start();

            String mainResourcesDirectory = Path.of("src/main/resources").toAbsolutePath().toString();
            try (ConfigurableApplicationContext context = new SpringApplicationBuilder(BackendApplication.class)
                    .profiles("dev")
                    .properties(
                            "spring.main.web-application-type=none",
                            "spring.config.location=optional:file:" + mainResourcesDirectory + "/"
                    )
                    .run(
                            "--spring.datasource.url=" + profileContainer.getJdbcUrl(),
                            "--spring.datasource.username=" + probeUsername,
                            "--spring.datasource.password=" + probePassword
                    )) {
                JdbcTemplate profileJdbcTemplate = context.getBean(JdbcTemplate.class);

                assertThat(context.getEnvironment().getActiveProfiles()).containsExactly("dev");
                assertThat(context.getEnvironment().getProperty("spring.flyway.locations"))
                        .isEqualTo("classpath:db/migration");
                assertThat(profileJdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class)).isZero();
                assertThat(profileJdbcTemplate.queryForObject("SELECT COUNT(*) FROM lodgings", Integer.class)).isZero();
            }
        }
    }

    private Flyway configuredDevSeedFlyway(ProbeDatabase probe, String adminPasswordHash) {
        return Flyway.configure()
                .dataSource(probe.jdbcUrl(), probe.username(), probe.password())
                .locations("classpath:db/migration", "classpath:db/dev")
                .placeholders(Map.of("dev_admin_password_hash", adminPasswordHash))
                .outOfOrder(true)
                .load();
    }

    private JdbcTemplate jdbcTemplateFor(ProbeDatabase probe) {
        return new JdbcTemplate(new org.springframework.jdbc.datasource.DriverManagerDataSource(
                probe.jdbcUrl(), probe.username(), probe.password()));
    }

    private String validDevAdminPasswordHash() {
        return validDevAdminPasswordHash;
    }

    private String validDevAdminPassword() {
        return DEV_ADMIN_PASSWORD;
    }

    private void assertSeedOwnedTablesAreEmpty(JdbcTemplate template) {
        for (String table : new String[]{
                "lodging_images", "lodging_features", "lodging_policies", "lodgings",
                "users", "categories", "features", "policies"
        }) {
            assertThat(template.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class))
                    .as(table)
                    .isZero();
        }
    }

    private void assertCanonicalSeedManifest(JdbcTemplate template) {
        Properties manifest = loadSeedManifest();
        assertProjectionHash(template, manifest, "categories", """
                SELECT id, name, description, icon FROM categories ORDER BY id
                """);
        assertProjectionHash(template, manifest, "features", """
                SELECT id, name, icon FROM features ORDER BY id
                """);
        assertProjectionHash(template, manifest, "policies", """
                SELECT id, name, description, icon FROM policies ORDER BY id
                """);
        assertProjectionHash(template, manifest, "users", """
                SELECT id, first_name, last_name, email, '<bcrypt>', role, image_url FROM users ORDER BY id
                """);
        assertProjectionHash(template, manifest, "lodgings", """
                SELECT id, name, description, address, city, country, phone_number, email,
                       category_id, price_per_night, max_guests, version
                FROM lodgings ORDER BY id
                """);
        assertProjectionHash(template, manifest, "lodging_images", """
                SELECT id, image_url, title, lodging_id FROM lodging_images ORDER BY id
                """);
        assertProjectionHash(template, manifest, "lodging_features", """
                SELECT lodging_id, feature_id FROM lodging_features ORDER BY lodging_id, feature_id
                """);
        assertProjectionHash(template, manifest, "lodging_policies", """
                SELECT lodging_id, policy_id FROM lodging_policies ORDER BY lodging_id, policy_id
                """);
    }

    private void assertProjectionHash(JdbcTemplate template, Properties manifest, String table, String query) {
        List<String> rows = template.query(query, (resultSet, rowNumber) -> {
            int columnCount = resultSet.getMetaData().getColumnCount();
            String[] values = new String[columnCount];
            for (int column = 1; column <= columnCount; column++) {
                values[column - 1] = resultSet.getString(column);
                if (values[column - 1] == null) {
                    values[column - 1] = "␀";
                }
            }
            return String.join("\u001f", values);
        });

        assertThat(sha256(String.join("\n", rows)))
                .as("canonical projection for " + table)
                .isEqualTo(manifest.getProperty(table));
    }

    private Properties loadSeedManifest() {
        Properties manifest = new Properties();
        try (InputStream input = getClass().getResourceAsStream("/fixtures/dev-seed-manifest.properties")) {
            if (input == null) {
                throw new IllegalStateException("Missing canonical dev seed manifest");
            }
            manifest.load(input);
            return manifest;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load canonical dev seed manifest", exception);
        }
    }

    private String[] loadV2Statements() {
        try (InputStream input = getClass().getResourceAsStream("/db/migration/V2__refresh_session_families.sql")) {
            if (input == null) {
                throw new IllegalStateException("Missing V2 migration");
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8).trim().split(";\\s*");
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load V2 migration", exception);
        }
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
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
