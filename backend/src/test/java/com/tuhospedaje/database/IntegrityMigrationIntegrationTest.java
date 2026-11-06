package com.tuhospedaje.database;

import com.tuhospedaje.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * P13 database integrity checks:
 * 1. ratings uniqueness: (lodging_id, user_id) must be unique.
 * 2. reservations version: must be NOT NULL DEFAULT 0.
 * 3. reservations date range: check_out must be strictly greater than check_in.
 */
@SpringBootTest
class IntegrityMigrationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void rejectsDuplicateRatingForSameUserAndLodging() {
        jdbcTemplate.update("INSERT INTO users (id, email, first_name, last_name, password, role) VALUES (9901, 'user9901@test.com', 'U', 'N', 'p', 'USER')");
        jdbcTemplate.update("INSERT INTO lodgings (id, name, description, address, city, country, phone_number, email, price_per_night, max_guests) VALUES (9901, 'L9901', 'D', 'A', 'C', 'Argentina', '+54000', 'l9901@test.com', 100.0, 2)");

        jdbcTemplate.update("INSERT INTO ratings (lodging_id, user_id, score, created_at) VALUES (9901, 9901, 5, NOW())");

        assertThatThrownBy(() -> jdbcTemplate.update("INSERT INTO ratings (lodging_id, user_id, score, created_at) VALUES (9901, 9901, 4, NOW())"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsReservationWithCheckOutNotStrictlyAfterCheckIn() {
        jdbcTemplate.update("INSERT INTO users (id, email, first_name, last_name, password, role) VALUES (9902, 'user9902@test.com', 'U', 'N', 'p', 'USER')");
        jdbcTemplate.update("INSERT INTO lodgings (id, name, description, address, city, country, phone_number, email, price_per_night, max_guests) VALUES (9902, 'L9902', 'D', 'A', 'C', 'Argentina', '+54000', 'l9902@test.com', 100.0, 2)");

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO reservations (lodging_id, user_id, check_in, check_out, total_price,
                                          guest_email, guest_name, guest_phone, status, created_at, created_at_derived)
                VALUES (9902, 9902, '2026-10-15', '2026-10-15', 100.0, 'g@test.com', 'G', '+54000', 'CONFIRMED', NOW(), 0)
                """)).isInstanceOf(DataIntegrityViolationException.class);

    }

    @Test
    void rejectsReservationWithCheckOutBeforeCheckIn() {
        jdbcTemplate.update("INSERT INTO users (id, email, first_name, last_name, password, role) VALUES (9902, 'user9902@test.com', 'U', 'N', 'p', 'USER')");
        jdbcTemplate.update("INSERT INTO lodgings (id, name, description, address, city, country, phone_number, email, price_per_night, max_guests) VALUES (9902, 'L9902', 'D', 'A', 'C', 'Argentina', '+54000', 'l9902@test.com', 100.0, 2)");
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO reservations (lodging_id, user_id, check_in, check_out, total_price,
                                          guest_email, guest_name, guest_phone, status, created_at, created_at_derived)
                VALUES (9902, 9902, '2026-10-15', '2026-10-10', 100.0, 'g@test.com', 'G', '+54000', 'CONFIRMED', NOW(), 0)
                """)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsReservationWithNullVersion() {
        jdbcTemplate.update("INSERT INTO users (id, email, first_name, last_name, password, role) VALUES (9903, 'user9903@test.com', 'U', 'N', 'p', 'USER')");
        jdbcTemplate.update("INSERT INTO lodgings (id, name, description, address, city, country, phone_number, email, price_per_night, max_guests) VALUES (9903, 'L9903', 'D', 'A', 'C', 'Argentina', '+54000', 'l9903@test.com', 100.0, 2)");

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO reservations (lodging_id, user_id, check_in, check_out, total_price,
                                          guest_email, guest_name, guest_phone, status, created_at, created_at_derived, version)
                VALUES (9903, 9903, '2026-10-15', '2026-10-18', 300.0, 'g@test.com', 'G', '+54000', 'CONFIRMED', NOW(), 0, NULL)
                """)).isInstanceOf(DataIntegrityViolationException.class);

    }

    @Test
    void defaultsReservationVersionToZero() {
        jdbcTemplate.update("INSERT INTO users (id, email, first_name, last_name, password, role) VALUES (9903, 'user9903@test.com', 'U', 'N', 'p', 'USER')");
        jdbcTemplate.update("INSERT INTO lodgings (id, name, description, address, city, country, phone_number, email, price_per_night, max_guests) VALUES (9903, 'L9903', 'D', 'A', 'C', 'Argentina', '+54000', 'l9903@test.com', 100.0, 2)");
        jdbcTemplate.update("""
                INSERT INTO reservations (id, lodging_id, user_id, check_in, check_out, total_price,
                                          guest_email, guest_name, guest_phone, status, created_at, created_at_derived)
                VALUES (9903, 9903, 9903, '2026-10-15', '2026-10-18', 300.0, 'g@test.com', 'G', '+54000', 'CONFIRMED', NOW(), 0)
                """);
        Long version = jdbcTemplate.queryForObject("SELECT version FROM reservations WHERE id = 9903", Long.class);
        assertThat(version).isEqualTo(0L);
    }
}
