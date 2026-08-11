package com.tuhospedaje.email.outbox;

import com.tuhospedaje.configuration.TestcontainersConfiguration;
import com.tuhospedaje.dto.auth.RegisterRequest;
import com.tuhospedaje.dto.reservation.CreateReservationRequest;
import com.tuhospedaje.entity.Lodging;
import com.tuhospedaje.entity.Reservation;
import com.tuhospedaje.entity.User;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class EmailOutboxEnqueueRollbackIntegrationTest {

    private static final String EMAIL_PREFIX = "phase2-rollback-";

    @Autowired
    private AuthService authService;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private LodgingRepository lodgingRepository;

    @Autowired
    private EmailOutboxRepository emailOutboxRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private EmailOutboxService emailOutboxService;

    @BeforeEach
    @AfterEach
    void cleanFixtures() {
        reset(emailOutboxService);
        jdbcTemplate.update("DELETE FROM reservations WHERE user_id IN "
                + "(SELECT id FROM users WHERE email LIKE ?)", EMAIL_PREFIX + "%");
        jdbcTemplate.update("DELETE FROM email_outbox WHERE user_id IN "
                + "(SELECT id FROM users WHERE email LIKE ?)", EMAIL_PREFIX + "%");
        jdbcTemplate.update("DELETE FROM users WHERE email LIKE ?", EMAIL_PREFIX + "%");
        jdbcTemplate.update("DELETE FROM lodgings WHERE email LIKE ?", EMAIL_PREFIX + "%");
    }

    @Test
    void welcomeEnqueueFailureRollsBackRegistration() {
        String email = EMAIL_PREFIX + "welcome@test.com";
        doThrow(new IllegalStateException("outbox unavailable"))
                .when(emailOutboxService).enqueueWelcome(any(), any(RegisterRequest.class));

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("Ana", "Gomez", email, "secret123")))
                .isInstanceOf(IllegalStateException.class);

        assertThat(userRepository.findByEmail(email)).isEmpty();
    }

    @Test
    void confirmationEnqueueFailureRollsBackReservationCreation() {
        User user = saveUser("create@test.com");
        Lodging lodging = saveLodging("create@test.com");
        doThrow(new IllegalStateException("outbox unavailable"))
                .when(emailOutboxService).enqueueReservationConfirmation(any(), any());

        assertThatThrownBy(() -> reservationService.createReservation(user,
                request(lodging.getId(), "create@test.com")))
                .isInstanceOf(IllegalStateException.class);

        assertThat(reservationRepository.findAll()).isEmpty();
        assertThat(emailOutboxRepository.findAll()).isEmpty();
    }

    @Test
    void cancellationEnqueueFailureRollsBackCancellation() {
        User user = saveUser("cancel@test.com");
        Lodging lodging = saveLodging("cancel@test.com");
        Reservation reservation = saveReservation(user, lodging);
        doThrow(new IllegalStateException("outbox unavailable"))
                .when(emailOutboxService).enqueueReservationCancellation(any(), any());

        assertThatThrownBy(() -> reservationService.cancelReservation(reservation.getId(), user))
                .isInstanceOf(IllegalStateException.class);

        assertThat(reservationRepository.findById(reservation.getId()).orElseThrow().getStatus())
                .isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(emailOutboxRepository.findAll()).isEmpty();
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

    private Reservation saveReservation(User user, Lodging lodging) {
        Reservation reservation = new Reservation();
        reservation.setUser(user);
        reservation.setLodging(lodging);
        reservation.setCheckIn(LocalDate.now().plusDays(10));
        reservation.setCheckOut(LocalDate.now().plusDays(12));
        reservation.setGuestName("Test Guest");
        reservation.setGuestEmail(EMAIL_PREFIX + "cancel@test.com");
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
