package com.tuhospedaje.database;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.mariadb.MariaDBContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IntegrityUpgradeIntegrationTest {

    @Test
    void upgradesLegacyReservationsWithoutChangingExistingVersions() {
        try (MariaDBContainer database = new MariaDBContainer("mariadb:10.11")) {
            database.start();
            flyway(database, "12").migrate();
            JdbcTemplate jdbc = jdbc(database);
            insertParents(jdbc);
            insertReservation(jdbc, 1, "2026-10-15", "2026-10-18", null);
            insertReservation(jdbc, 2, "2026-10-20", "2026-10-22", 7L);

            assertThat(latestFlyway(database).migrate().migrationsExecuted).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT version FROM reservations WHERE id = 1", Long.class)).isZero();
            assertThat(jdbc.queryForObject("SELECT version FROM reservations WHERE id = 2", Long.class)).isEqualTo(7L);
        }
    }

    @Test
    void rejectsConflictingHistoricalDataBeforeChangingSchemaOrRows() {
        try (MariaDBContainer database = new MariaDBContainer("mariadb:10.11")) {
            database.start();
            flyway(database, "12").migrate();
            JdbcTemplate jdbc = jdbc(database);
            insertParents(jdbc);
            insertReservation(jdbc, 1, "2026-10-15", "2026-10-15", null);
            jdbc.update("INSERT INTO ratings (lodging_id, user_id, score, created_at) VALUES (1, 1, 5, NOW()), (1, 1, 4, NOW())");

            assertThatThrownBy(() -> latestFlyway(database).migrate())
                    .isInstanceOf(RuntimeException.class)
                    .hasStackTraceContaining("V13 blocked: duplicate ratings require manual resolution");
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ratings", Integer.class)).isEqualTo(2);
            assertThat(jdbc.queryForObject("SELECT version FROM reservations WHERE id = 1", Long.class)).isNull();
            assertThat(jdbc.queryForObject("""
                    SELECT IS_NULLABLE FROM information_schema.columns
                    WHERE table_schema = DATABASE() AND table_name = 'reservations' AND column_name = 'version'
                    """, String.class)).isEqualTo("YES");
            assertThat(jdbc.queryForObject("""
                    SELECT COUNT(*) FROM information_schema.statistics
                    WHERE table_schema = DATABASE() AND table_name = 'ratings'
                      AND index_name = 'UK_ratings_lodging_user'
                    """, Integer.class)).isZero();

            // Verify that failed migration leaves flyway_schema_history in failed state
            Integer failedCount = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '13' AND success = 0", Integer.class);
            assertThat(failedCount).isEqualTo(1);
        }
    }

    @Test
    void rejectsInvalidHistoricalDatesBeforeChangingSchemaOrRows() {
        try (MariaDBContainer database = new MariaDBContainer("mariadb:10.11")) {
            database.start();
            flyway(database, "12").migrate();
            JdbcTemplate jdbc = jdbc(database);
            insertParents(jdbc);
            insertReservation(jdbc, 1, "2026-10-15", "2026-10-14", null);

            assertThatThrownBy(() -> latestFlyway(database).migrate())
                    .isInstanceOf(RuntimeException.class)
                    .hasStackTraceContaining("V13 blocked: invalid reservation dates require manual resolution");
            assertThat(jdbc.queryForObject("SELECT version FROM reservations WHERE id = 1", Long.class)).isNull();
            assertThat(jdbc.queryForObject("SELECT check_out FROM reservations WHERE id = 1", java.sql.Date.class))
                    .isEqualTo(java.sql.Date.valueOf("2026-10-14"));
            assertThat(jdbc.queryForObject("""
                    SELECT IS_NULLABLE FROM information_schema.columns
                    WHERE table_schema = DATABASE() AND table_name = 'reservations' AND column_name = 'version'
                    """, String.class)).isEqualTo("YES");
        }
    }

    @Test
    void retriesV13AfterVersionModificationCommittedByPartialDdl() {
        try (MariaDBContainer database = new MariaDBContainer("mariadb:10.11")) {
            database.start();
            flyway(database, "12").migrate();
            JdbcTemplate jdbc = jdbc(database);
            insertParents(jdbc);
            insertReservation(jdbc, 1, "2026-10-15", "2026-10-18", null);

            // Simulate partial DDL failure: version update and modify committed before migration completed
            jdbc.execute("UPDATE reservations SET version = 0 WHERE version IS NULL");
            jdbc.execute("ALTER TABLE reservations MODIFY version BIGINT NOT NULL DEFAULT 0");

            assertThat(latestFlyway(database).migrate().migrationsExecuted).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT version FROM reservations WHERE id = 1", Long.class)).isZero();
            assertThat(jdbc.queryForObject("""
                    SELECT COUNT(*) FROM information_schema.statistics
                    WHERE table_schema = DATABASE() AND table_name = 'ratings' AND index_name = 'UK_ratings_lodging_user'
                    """, Integer.class)).isGreaterThan(0);
        }
    }

    @Test
    void retriesV13AfterUniqueIndexCommittedByPartialDdl() {
        try (MariaDBContainer database = new MariaDBContainer("mariadb:10.11")) {
            database.start();
            flyway(database, "12").migrate();
            JdbcTemplate jdbc = jdbc(database);
            insertParents(jdbc);
            insertReservation(jdbc, 1, "2026-10-15", "2026-10-18", null);

            // Simulate partial DDL failure: statements 1, 2, and 3 committed
            jdbc.execute("UPDATE reservations SET version = 0 WHERE version IS NULL");
            jdbc.execute("ALTER TABLE reservations MODIFY version BIGINT NOT NULL DEFAULT 0");
            jdbc.execute("CREATE UNIQUE INDEX IF NOT EXISTS UK_ratings_lodging_user ON ratings (lodging_id, user_id)");

            assertThat(latestFlyway(database).migrate().migrationsExecuted).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT version FROM reservations WHERE id = 1", Long.class)).isZero();
            assertThat(jdbc.queryForObject("""
                    SELECT COUNT(*) FROM information_schema.check_constraints
                    WHERE constraint_schema = DATABASE() AND constraint_name = 'CK_reservations_date_range'
                    """, Integer.class)).isGreaterThan(0);
        }
    }

    private static Flyway flyway(MariaDBContainer database, String target) {
        return Flyway.configure().dataSource(database.getJdbcUrl(), database.getUsername(), database.getPassword())
                .locations("classpath:db/migration").target(target).load();
    }

    private static Flyway latestFlyway(MariaDBContainer database) {
        return Flyway.configure().dataSource(database.getJdbcUrl(), database.getUsername(), database.getPassword())
                .locations("classpath:db/migration").load();
    }

    private static JdbcTemplate jdbc(MariaDBContainer database) {
        return new JdbcTemplate(new DriverManagerDataSource(
                database.getJdbcUrl(), database.getUsername(), database.getPassword()));
    }

    private static void insertParents(JdbcTemplate jdbc) {
        jdbc.update("INSERT INTO users (id, email, first_name, last_name, password, role) VALUES (1, 'legacy@example.com', 'Legacy', 'User', 'hash', 'USER')");
        jdbc.update("""
                INSERT INTO lodgings (id, name, address, city, country, phone_number, email, price_per_night, max_guests)
                VALUES (1, 'Legacy lodging', 'Address', 'City', 'Argentina', '+54000', 'lodging@example.com', 100, 2)
                """);
    }

    private static void insertReservation(JdbcTemplate jdbc, int id, String checkIn, String checkOut, Long version) {
        jdbc.update("""
                INSERT INTO reservations (id, lodging_id, user_id, check_in, check_out, total_price,
                    guest_email, guest_name, guest_phone, status, created_at, created_at_derived, version)
                VALUES (?, 1, 1, ?, ?, 100, 'guest@example.com', 'Guest', '+54000', 'CONFIRMED', NOW(), 0, ?)
                """, id, checkIn, checkOut, version);
    }
}
