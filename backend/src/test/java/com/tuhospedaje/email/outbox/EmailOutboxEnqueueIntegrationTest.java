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
import com.tuhospedaje.service.EmailTransportFailureClassification;
import com.tuhospedaje.service.ReservationService;
import com.tuhospedaje.service.impl.EmailOutboxTransactionService;
import com.tuhospedaje.service.impl.RegistrationPersistenceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class EmailOutboxEnqueueIntegrationTest {

    private static final String EMAIL_PREFIX = "phase2-enqueue-";
    private static final String CONCURRENT_EMAIL = EMAIL_PREFIX + "concurrent@test.com";
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
    @MockitoSpyBean
    private RegistrationPersistenceService registrationPersistenceService;
    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private LodgingRepository lodgingRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private EmailOutboxTransactionService emailOutboxTransactions;

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
    void registrationCommitsOneSpanishWelcomeOutboxRowWithTheBusinessChange() {
        String email = EMAIL_PREFIX + "welcome@test.com";

        AuthService.AuthResult result = authService.register(
                new RegisterRequest("Ana <b>Gómez</b>", "Gomez", email, "secret123"));

        User user = userRepository.findByEmail(email).orElseThrow();
        List<com.tuhospedaje.entity.EmailOutbox> rows = emailOutboxRepository.findAll().stream()
                .filter(row -> row.getUser().getId().equals(user.getId()))
                .toList();

        assertThat(result.body().getEmail()).isEqualTo(email);
        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.getEmailType()).isEqualTo("WELCOME");
            assertThat(row.getAggregateId()).isEqualTo(user.getId().toString());
            assertThat(row.getStatus()).isEqualTo(EmailOutboxStatus.PENDING);
            assertThat(row.getRecipient()).isEqualTo(email);
            assertThat(row.getSubject()).isEqualTo("¡Bienvenido a TuHospedaje!");
            assertThat(row.getHtmlBody())
                    .contains("href=\"https://app.test/login\"")
                    .contains("Ana &lt;b&gt;")
                    .doesNotContain("localhost", "<b>Gómez</b>", "Welcome", "Thanks");
        });
    }

    @Test
    void terminalWelcomeFailureAfterRegistrationDoesNotChangeCommittedAuthOutcome() {
        String email = EMAIL_PREFIX + "terminal-failure@test.com";
        AuthService.AuthResult result = authService.register(
                new RegisterRequest("Ana", "Gómez", email, "secret123"));
        User user = userRepository.findByEmail(email).orElseThrow();
        com.tuhospedaje.entity.EmailOutbox outbox = emailOutboxRepository.findByEmailTypeAndAggregateId(
                "WELCOME", user.getId().toString()).orElseThrow();
        String token = UUID.randomUUID().toString();
        outbox.setStatus(EmailOutboxStatus.PROCESSING);
        outbox.setLeaseToken(token);
        outbox.setLeaseUntil(Instant.now().plusSeconds(60));
        emailOutboxRepository.saveAndFlush(outbox);

        int updated = emailOutboxTransactions.markFailed(
                outbox.getId(), token, EmailTransportFailureClassification.SMTP_UNAVAILABLE, Instant.now());

        assertThat(updated).isEqualTo(1);
        assertThat(emailOutboxRepository.findById(outbox.getId()).orElseThrow())
                .satisfies(failed -> {
                    assertThat(failed.getStatus()).isEqualTo(EmailOutboxStatus.FAILED);
                    assertThat(failed.getFailedAttempts()).isEqualTo(1);
                });
        assertThat(userRepository.findByEmail(email).orElseThrow().getId()).isEqualTo(user.getId());
        assertThat(authService.currentUser(email).getEmail()).isEqualTo(email);
        assertThat(result.body().getEmail()).isEqualTo(email);
        assertThat(result.token()).isNotBlank();
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

    @Test
    void concurrentDuplicateRegistrationPreservesTheEstablishedDuplicateOutcome() throws Exception {
        CountDownLatch bothRequestsReachedDuplicateCheck = new CountDownLatch(2);
        doAnswer(invocation -> {
            bothRequestsReachedDuplicateCheck.countDown();
            assertThat(bothRequestsReachedDuplicateCheck.await(5, TimeUnit.SECONDS)).isTrue();
            return invocation.callRealMethod();
        }).when(registrationPersistenceService).persist(any(RegisterRequest.class), any(String.class));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Object> register = () -> {
                try {
                    return authService.register(new RegisterRequest("Ana", "Gómez", CONCURRENT_EMAIL, "secret123"));
                } catch (RuntimeException exception) {
                    return exception;
                }
            };
            Future<Object> first = executor.submit(register);
            Future<Object> second = executor.submit(register);
            List<Object> outcomes = List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));

            assertThat(outcomes.stream().filter(AuthService.AuthResult.class::isInstance)).hasSize(1);
            assertThat(outcomes.stream().filter(IllegalArgumentException.class::isInstance)
                    .map(IllegalArgumentException.class::cast)
                    .map(Throwable::getMessage))
                    .containsExactly("El email ya está registrado");
        } finally {
            executor.shutdownNow();
        }

        assertThat(userRepository.findByEmail(CONCURRENT_EMAIL)).isPresent();
        assertThat(emailOutboxRepository.findAll().stream()
                .filter(row -> row.getEmailType().equals("WELCOME") && row.getRecipient().equals(CONCURRENT_EMAIL)))
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
