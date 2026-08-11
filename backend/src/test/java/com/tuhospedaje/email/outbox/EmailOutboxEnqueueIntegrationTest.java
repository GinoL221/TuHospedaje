package com.tuhospedaje.email.outbox;

import com.tuhospedaje.configuration.TestcontainersConfiguration;
import com.tuhospedaje.dto.auth.RegisterRequest;
import com.tuhospedaje.dto.reservation.CreateReservationRequest;
import com.tuhospedaje.entity.Lodging;
import com.tuhospedaje.entity.Reservation;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.EmailOutboxStatus;
import com.tuhospedaje.enums.ReservationStatus;
import com.tuhospedaje.enums.RoleEnum;
import com.tuhospedaje.repository.EmailOutboxRepository;
import com.tuhospedaje.repository.LodgingRepository;
import com.tuhospedaje.repository.ReservationRepository;
import com.tuhospedaje.repository.UserRepository;
import com.tuhospedaje.service.AuthService;
import com.tuhospedaje.service.EmailOutboxService;
import com.tuhospedaje.service.ReservationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class EmailOutboxEnqueueIntegrationTest {

    private static final String EMAIL_PREFIX = "phase2-enqueue-";
    @Autowired
    private AuthService authService;
    @Autowired
    private ReservationService reservationService;
    @Autowired
    private EmailOutboxService emailOutboxService;
    @Autowired
    private EmailOutboxRepository emailOutboxRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private LodgingRepository lodgingRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    @AfterEach
    void cleanFixtures() {
        jdbcTemplate.update("DELETE FROM reservations WHERE user_id IN "
                + "(SELECT id FROM users WHERE email LIKE ?)", EMAIL_PREFIX + "%");
        jdbcTemplate.update("DELETE FROM email_outbox WHERE user_id IN "
                + "(SELECT id FROM users WHERE email LIKE ?)", EMAIL_PREFIX + "%");
        jdbcTemplate.update("DELETE FROM users WHERE email LIKE ?", EMAIL_PREFIX + "%");
        jdbcTemplate.update("DELETE FROM lodgings WHERE email LIKE ?", EMAIL_PREFIX + "%");
    }

    @Test
    void registrationCommitsOneWelcomeOutboxRowWithTheBusinessChange() {
        String email = EMAIL_PREFIX + "welcome@test.com";

        AuthService.AuthResult result = authService.register(
                new RegisterRequest("Ana", "Gomez", email, "secret123"));

        User user = userRepository.findByEmail(email).orElseThrow();
        List<com.tuhospedaje.entity.EmailOutbox> rows = emailOutboxRepository.findAll().stream()
                .filter(row -> row.getUser().getId().equals(user.getId()))
                .toList();

        assertThat(result.body().getEmail()).isEqualTo(email);
        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.getEmailType()).isEqualTo("WELCOME");
            assertThat(row.getAggregateId()).isEqualTo(user.getId().toString());
            assertThat(row.getStatus()).isEqualTo(EmailOutboxStatus.PENDING);
        });
    }

    @Test
    void reservationCreationCommitsOneConfirmationOutboxRow() {
        User user = saveUser("create@test.com");
        Lodging lodging = saveLodging("create@test.com");
        CreateReservationRequest request = request(lodging.getId(), "create@test.com");

        var response = reservationService.createReservation(user, request);

        assertThat(reservationRepository.findById(response.getId())).isPresent();
        assertThat(emailOutboxRepository.findByEmailTypeAndAggregateId(
                "RESERVATION_CONFIRMATION", response.getId().toString())).isPresent();
    }

    @Test
    void reservationCancellationCommitsOneCancellationOutboxRow() {
        User user = saveUser("cancel@test.com");
        Lodging lodging = saveLodging("cancel@test.com");
        Reservation reservation = saveReservation(user, lodging, "cancel@test.com");

        var response = reservationService.cancelReservation(reservation.getId(), user);

        assertThat(response.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(emailOutboxRepository.findByEmailTypeAndAggregateId(
                "RESERVATION_CANCELLATION", reservation.getId().toString())).isPresent();
    }

    @Test
    void repeatedEnqueueUsesTheAggregateAndTypeUniquenessGuard() {
        User user = saveUser("duplicate@test.com");
        RegisterRequest request = new RegisterRequest("Ana", "Gomez", user.getEmail(), "secret123");

        emailOutboxService.enqueueWelcome(user, request);
        emailOutboxService.enqueueWelcome(user, request);

        assertThat(emailOutboxRepository.findAll().stream()
                .filter(row -> row.getEmailType().equals("WELCOME")
                        && row.getAggregateId().equals(user.getId().toString())))
                .hasSize(1);
    }

    private User saveUser(String suffix) {
        return userRepository.save(User.builder()
                .firstName("Test")
                .lastName("User")
                .email(EMAIL_PREFIX + suffix)
                .password("hash")
                .role(RoleEnum.USER)
                .build());
    }

    private Lodging saveLodging(String suffix) {
        Lodging lodging = new Lodging();
        lodging.setName("Phase 2 lodging");
        lodging.setDescription("Test lodging");
        lodging.setAddress("Test Street");
        lodging.setCity("Buenos Aires");
        lodging.setCountry("Argentina");
        lodging.setPhoneNumber("123");
        lodging.setEmail(EMAIL_PREFIX + suffix);
        lodging.setPricePerNight(new BigDecimal("100.00"));
        lodging.setMaxGuests(2);
        return lodgingRepository.save(lodging);
    }

    private Reservation saveReservation(User user, Lodging lodging, String suffix) {
        Reservation reservation = new Reservation();
        reservation.setUser(user);
        reservation.setLodging(lodging);
        reservation.setCheckIn(LocalDate.now().plusDays(10));
        reservation.setCheckOut(LocalDate.now().plusDays(12));
        reservation.setGuestName("Test Guest");
        reservation.setGuestEmail(EMAIL_PREFIX + suffix);
        reservation.setGuestPhone("123");
        reservation.setTotalPrice(new BigDecimal("200.00"));
        reservation.setStatus(ReservationStatus.CONFIRMED);
        return reservationRepository.save(reservation);
    }

    private CreateReservationRequest request(Long lodgingId, String suffix) {
        CreateReservationRequest request = new CreateReservationRequest();
        request.setLodgingId(lodgingId);
        request.setCheckIn(LocalDate.now().plusDays(10));
        request.setCheckOut(LocalDate.now().plusDays(12));
        request.setGuestName("Test Guest");
        request.setGuestEmail(EMAIL_PREFIX + suffix);
        request.setGuestPhone("123");
        return request;
    }
}
