package com.tuhospedaje.reservation;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.MariaDBContainer;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationMigrationIntegrationTest {

    @Test
    void v7BackfillsLegacyReservationsAndAppliesDefaultsForNewRows() {
        try (MariaDBContainer<?> database = new MariaDBContainer<>("mariadb:10.11")) {
            database.start();
            Flyway.configure()
                    .dataSource(database.getJdbcUrl(), database.getUsername(), database.getPassword())
                    .locations("classpath:db/migration")
                    .target("6")
                    .load()
                    .migrate();
            JdbcTemplate jdbcTemplate = new JdbcTemplate(new org.springframework.jdbc.datasource.DriverManagerDataSource(
                    database.getJdbcUrl(), database.getUsername(), database.getPassword()));
            jdbcTemplate.update("""
                    INSERT INTO users (email, first_name, last_name, password, role)
                    VALUES ('legacy-user@example.com', 'Legacy', 'User', 'hash', 'USER')
                    """);
            jdbcTemplate.update("""
                    INSERT INTO lodgings (name, description, address, city, country, phone_number, email,
                                          price_per_night, max_guests)
                    VALUES ('Legacy lodging', 'Description', 'Address', 'City', 'Argentina', '+54000000000',
                            'legacy-lodging@example.com', 100.00, 2)
                    """);
            jdbcTemplate.update("""
                    INSERT INTO reservations (check_in, check_out, total_price, lodging_id, user_id,
                                              guest_email, guest_name, guest_phone, status)
                    VALUES ('2026-09-15', '2026-09-17', 200.00, 1, 1, 'legacy-user@example.com',
                            'Legacy User', '+54000000000', 'CONFIRMED')
                    """);

            int migrationsExecuted = Flyway.configure()
                    .dataSource(database.getJdbcUrl(), database.getUsername(), database.getPassword())
                    .locations("classpath:db/migration")
                    .load()
                    .migrate()
                    .migrationsExecuted;

            assertThat(migrationsExecuted).isEqualTo(1);
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT notes FROM reservations WHERE id = 1", String.class)).isNull();
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT created_at FROM reservations WHERE id = 1", Timestamp.class).toLocalDateTime())
                    .isEqualTo(LocalDateTime.of(2026, 9, 15, 0, 0));
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT created_at_derived FROM reservations WHERE id = 1", Boolean.class)).isTrue();
            assertThat(jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM information_schema.columns
                    WHERE table_schema = DATABASE() AND table_name = 'reservations'
                      AND column_name IN ('notes', 'created_at', 'created_at_derived')
                      AND is_nullable = 'NO'
                    """, Integer.class)).isEqualTo(2);
        }
    }
}
