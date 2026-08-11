package com.tuhospedaje.reservation;

import com.tuhospedaje.AbstractIntegrationTest;
import com.tuhospedaje.dto.reservation.ReservationResponse;
import com.tuhospedaje.entity.Lodging;
import com.tuhospedaje.entity.Reservation;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.ReservationStatus;
import com.tuhospedaje.enums.RoleEnum;
import com.tuhospedaje.repository.LodgingRepository;
import com.tuhospedaje.repository.RatingRepository;
import com.tuhospedaje.repository.ReservationRepository;
import com.tuhospedaje.repository.UserRepository;
import com.tuhospedaje.service.EmailOutboxService;
import com.tuhospedaje.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ReservationCancellationConcurrencyTest extends AbstractIntegrationTest {

    @Autowired ReservationService reservationService;
    @Autowired ReservationRepository reservationRepository;
    @Autowired LodgingRepository lodgingRepository;
    @Autowired RatingRepository ratingRepository;
    @Autowired UserRepository userRepository;
    @Autowired PlatformTransactionManager transactionManager;
    @MockitoBean EmailOutboxService emailOutboxService;

    private User owner;
    private Long reservationId;

    @BeforeEach
    void setUp() {
        ratingRepository.deleteAll();
        reservationRepository.deleteAll();
        lodgingRepository.deleteAll();
        userRepository.deleteAll();
        owner = userRepository.save(User.builder()
                .firstName("Concurrent").lastName("Owner")
                .email("concurrent-owner@test.com").password("hash")
                .role(RoleEnum.USER).build());
        Lodging lodging = new Lodging();
        lodging.setName("Concurrent lodging");
        lodging.setDescription("desc");
        lodging.setAddress("Street 1");
        lodging.setCity("Buenos Aires");
        lodging.setCountry("Argentina");
        lodging.setPhoneNumber("123");
        lodging.setEmail("lodging@test.com");
        lodging.setPricePerNight(new BigDecimal("100.00"));
        lodging.setMaxGuests(2);
        lodging = lodgingRepository.save(lodging);
        Reservation reservation = new Reservation();
        reservation.setUser(owner);
        reservation.setLodging(lodging);
        reservation.setCheckIn(LocalDate.now().plusDays(10));
        reservation.setCheckOut(LocalDate.now().plusDays(12));
        reservation.setGuestName("Guest");
        reservation.setGuestEmail("guest@test.com");
        reservation.setGuestPhone("123");
        reservation.setTotalPrice(new BigDecimal("200.00"));
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservationId = reservationRepository.save(reservation).getId();
    }

    @Test
    void concurrentRequestsProduceOneTransitionOneEmailAndCancelledResponses() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        TransactionTemplate transactions = new TransactionTemplate(transactionManager);

        var executor = Executors.newFixedThreadPool(2);
        try {
            var calls = List.of(1, 2).stream()
                    .map(ignored -> executor.submit(() -> {
                        ready.countDown();
                        assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
                        return transactions.execute(status ->
                                reservationService.cancelReservation(reservationId, owner));
                    }))
                    .toList();
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<ReservationResponse> responses = calls.stream()
                    .map(future -> {
                        try {
                            return future.get(20, TimeUnit.SECONDS);
                        } catch (Exception ex) {
                            throw new AssertionError(ex);
                        }
                    })
                    .toList();

            assertThat(responses).extracting(ReservationResponse::getStatus)
                    .containsOnly(ReservationStatus.CANCELLED);
        } finally {
            executor.shutdownNow();
        }

        assertThat(reservationRepository.findById(reservationId).orElseThrow().getStatus())
                .isEqualTo(ReservationStatus.CANCELLED);
        verify(emailOutboxService, times(1)).enqueueReservationCancellation(any(), any(ReservationResponse.class));
    }
}
