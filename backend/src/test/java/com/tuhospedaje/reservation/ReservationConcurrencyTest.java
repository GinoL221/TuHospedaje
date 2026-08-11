package com.tuhospedaje.reservation;

import com.tuhospedaje.configuration.TestcontainersConfiguration;
import com.tuhospedaje.dto.reservation.CreateReservationRequest;
import com.tuhospedaje.entity.Lodging;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.ReservationStatus;
import com.tuhospedaje.enums.RoleEnum;
import com.tuhospedaje.repository.LodgingRepository;
import com.tuhospedaje.repository.RatingRepository;
import com.tuhospedaje.repository.ReservationRepository;
import com.tuhospedaje.repository.UserRepository;
import com.tuhospedaje.service.ReservationService;
import com.tuhospedaje.service.EmailOutboxService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrency test for reservation double-booking prevention (REQ-1, SC-1.2, SC-1.3).
 *
 * IMPORTANT: does NOT extend AbstractIntegrationTest. The class-level @Transactional
 * on AbstractIntegrationTest would wrap both threads in one rolled-back transaction and
 * defeat real commits. Each service call gets its own transaction here.
 *
 * Also covers SC-1.4 (adjacent dates, both succeed) and SC-1.5 (different lodgings, both succeed)
 * as non-concurrent integration tests.
 *
 * Empirical gap-lock verification: the two-thread test with an empty-reservations lodging
 * serves as empirical confirmation that InnoDB gap-locking on the lodging_id FK index
 * serializes concurrent inserts for the same lodging. If the test finds 2 CONFIRMED rows,
 * gap lock was insufficient and the fallback (PESSIMISTIC_WRITE on the parent Lodging row)
 * must be applied.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ReservationConcurrencyTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private LodgingRepository lodgingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private EmailOutboxService emailOutboxService;

    private Lodging seededLodging;
    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        ratingRepository.deleteAll();
        reservationRepository.deleteAll();
        jdbcTemplate.update("DELETE FROM email_outbox");
        userRepository.deleteAll();
        lodgingRepository.deleteAll();

        seededLodging = new Lodging();
        seededLodging.setName("Concurrency Test Lodging");
        seededLodging.setDescription("For concurrency testing");
        seededLodging.setAddress("Test Street 1");
        seededLodging.setCity("TestCity");
        seededLodging.setCountry("TestCountry");
        seededLodging.setPhoneNumber("123456789");
        seededLodging.setEmail("concurrency-lodging@test.com");
        seededLodging.setPricePerNight(new BigDecimal("100.00"));
        seededLodging.setMaxGuests(4);
        seededLodging = lodgingRepository.save(seededLodging);

        userA = userRepository.save(User.builder()
                .firstName("UserA")
                .lastName("Test")
                .email("usera-concurrency@test.com")
                .password("hashed")
                .role(RoleEnum.USER)
                .build());

        userB = userRepository.save(User.builder()
                .firstName("UserB")
                .lastName("Test")
                .email("userb-concurrency@test.com")
                .password("hashed")
                .role(RoleEnum.USER)
                .build());
    }

    @AfterEach
    void tearDown() {
        // Manual cleanup — no auto-rollback since this test is intentionally non-@Transactional
        ratingRepository.deleteAll();
        reservationRepository.deleteAll();
        jdbcTemplate.update("DELETE FROM email_outbox");
        userRepository.deleteAll();
        lodgingRepository.deleteAll();
    }

    /**
     * SC-1.2 / SC-1.3: Two concurrent requests for the same lodging with overlapping dates.
     * Exactly one must succeed; the other must be rejected.
     * DB must contain exactly 1 CONFIRMED reservation afterwards.
     *
     * This test also serves as empirical verification of InnoDB gap-locking:
     * if 2 CONFIRMED rows are found, the gap lock was insufficient and the
     * parent-row fallback must be applied.
     */
    @Test
    void concurrentOverlappingReservations_exactlyOneSucceeds() throws InterruptedException {
        LocalDate checkIn  = LocalDate.now().plusDays(30);
        LocalDate checkOut = LocalDate.now().plusDays(35);

        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        List<Future<Object>> futures = new ArrayList<>();

        futures.add(executor.submit(() -> {
            startGate.await();
            return callCreate(userA, seededLodging.getId(), checkIn, checkOut); // throws on failure
        }));

        futures.add(executor.submit(() -> {
            startGate.await();
            return callCreate(userB, seededLodging.getId(), checkIn, checkOut); // throws on failure
        }));

        startGate.countDown();
        executor.shutdown();
        boolean finished = executor.awaitTermination(30, TimeUnit.SECONDS);
        assertThat(finished).as("executor did not finish in time — possible deadlock").isTrue();

        long successCount = futures.stream().filter(f -> {
            try {
                Object result = f.get();
                return result != null && !(result instanceof Throwable);
            } catch (ExecutionException | InterruptedException e) {
                return false;
            }
        }).count();

        long failCount = futures.stream().filter(f -> {
            try {
                f.get();
                return false;
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                return cause instanceof IllegalArgumentException
                        || cause instanceof PessimisticLockingFailureException;
            } catch (InterruptedException e) {
                return false;
            }
        }).count();

        assertThat(successCount).as("exactly one reservation must succeed").isEqualTo(1);
        assertThat(failCount).as("exactly one reservation must be rejected").isEqualTo(1);

        long confirmedCount = reservationRepository
                .findByLodgingIdAndStatus(seededLodging.getId(), ReservationStatus.CONFIRMED)
                .size();
        assertThat(confirmedCount)
                .as("DB must contain exactly 1 CONFIRMED reservation after concurrent overlap — " +
                    "if 2 found, InnoDB gap lock was insufficient: apply parent-row PESSIMISTIC_WRITE fallback")
                .isEqualTo(1);
    }

    /**
     * SC-1.4: Adjacent non-overlapping dates (checkout == next checkin) — both must succeed.
     * Spec binding: checkout == checkin is NOT an overlap.
     */
    @Test
    void adjacentDateReservations_bothSucceed() {
        LocalDate firstCheckIn  = LocalDate.now().plusDays(40);
        LocalDate firstCheckOut = LocalDate.now().plusDays(45); // checkout Jun 45

        LocalDate secondCheckIn  = LocalDate.now().plusDays(45); // checkin same day = NOT overlap
        LocalDate secondCheckOut = LocalDate.now().plusDays(50);

        Object resultA = callCreateSafe(userA, seededLodging.getId(), firstCheckIn, firstCheckOut);
        Object resultB = callCreateSafe(userB, seededLodging.getId(), secondCheckIn, secondCheckOut);

        assertThat(resultA).as("first reservation (Jun 40-45) must succeed").isNotInstanceOf(Throwable.class);
        assertThat(resultB).as("second reservation (Jun 45-50) — adjacent, NOT overlap — must succeed")
                .isNotInstanceOf(Throwable.class);

        long confirmedCount = reservationRepository
                .findByLodgingIdAndStatus(seededLodging.getId(), ReservationStatus.CONFIRMED)
                .size();
        assertThat(confirmedCount).as("both adjacent reservations must be CONFIRMED").isEqualTo(2);
    }

    /**
     * SC-1.5: Concurrent requests for different lodgings — both must succeed independently.
     */
    @Test
    void differentLodgings_bothSucceedIndependently() {
        Lodging lodgingB = new Lodging();
        lodgingB.setName("Second Concurrency Lodging");
        lodgingB.setDescription("Different lodging");
        lodgingB.setAddress("Test Street 2");
        lodgingB.setCity("TestCity");
        lodgingB.setCountry("TestCountry");
        lodgingB.setPhoneNumber("987654321");
        lodgingB.setEmail("concurrency-lodging-b@test.com");
        lodgingB.setPricePerNight(new BigDecimal("200.00"));
        lodgingB.setMaxGuests(2);
        lodgingB = lodgingRepository.save(lodgingB);

        LocalDate checkIn  = LocalDate.now().plusDays(60);
        LocalDate checkOut = LocalDate.now().plusDays(65);

        Object resultA = callCreateSafe(userA, seededLodging.getId(), checkIn, checkOut);
        Object resultB = callCreateSafe(userB, lodgingB.getId(), checkIn, checkOut);

        assertThat(resultA).as("reservation for lodging A must succeed").isNotInstanceOf(Throwable.class);
        assertThat(resultB).as("reservation for lodging B must succeed").isNotInstanceOf(Throwable.class);

        assertThat(reservationRepository
                .findByLodgingIdAndStatus(seededLodging.getId(), ReservationStatus.CONFIRMED))
                .as("lodging A must have exactly 1 CONFIRMED reservation").hasSize(1);
        assertThat(reservationRepository
                .findByLodgingIdAndStatus(lodgingB.getId(), ReservationStatus.CONFIRMED))
                .as("lodging B must have exactly 1 CONFIRMED reservation").hasSize(1);
    }

    // ---- helpers ----

    /**
     * Builds and submits a reservation request. Returns the ReservationResponse on success.
     * Throws the original exception on failure so the calling Future records it properly.
     */
    private Object callCreate(User user, Long lodgingId, LocalDate checkIn, LocalDate checkOut)
            throws Exception {
        CreateReservationRequest req = new CreateReservationRequest();
        req.setLodgingId(lodgingId);
        req.setCheckIn(checkIn);
        req.setCheckOut(checkOut);
        req.setGuestName(user.getFirstName() + " " + user.getLastName());
        req.setGuestEmail(user.getEmail());
        req.setGuestPhone("+5491100000000");
        return reservationService.createReservation(user, req);
    }

    /**
     * Non-concurrent version used by SC-1.4 and SC-1.5. Returns the response or wraps
     * the exception in a RuntimeException with the original as cause.
     */
    private Object callCreateSafe(User user, Long lodgingId, LocalDate checkIn, LocalDate checkOut) {
        try {
            return callCreate(user, lodgingId, checkIn, checkOut);
        } catch (Exception e) {
            return e;
        }
    }
}
