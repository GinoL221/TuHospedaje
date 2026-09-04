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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.aop.framework.ProxyFactory;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import(ReservationCancellationConcurrencyTest.LockBoundaryTestConfiguration.class)
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
    @Autowired LockBoundaryGate lockBoundaryGate;

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
        lockBoundaryGate.arm(reservationId);
        TransactionTemplate transactions = new TransactionTemplate(transactionManager);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<Future<ReservationResponse>> calls = new ArrayList<>();
        try {
            calls.add(executor.submit(() -> transactions.execute(status ->
                    reservationService.cancelReservation(reservationId, owner))));
            calls.add(executor.submit(() -> transactions.execute(status ->
                    reservationService.cancelReservation(reservationId, owner))));
            lockBoundaryGate.awaitBothArrivals();
            lockBoundaryGate.release();

            List<ReservationResponse> responses = calls.stream().map(this::awaitResponse).toList();

            assertThat(responses).extracting(ReservationResponse::getStatus)
                    .containsOnly(ReservationStatus.CANCELLED);
        } finally {
            cleanup(executor, calls);
        }

        assertThat(reservationRepository.findById(reservationId).orElseThrow().getStatus())
                .isEqualTo(ReservationStatus.CANCELLED);
        verify(emailOutboxService, times(1)).enqueueReservationCancellation(any(), any(ReservationResponse.class));
    }

    @Test
    void boundaryArrivalTimeoutReleasesWaitingWorkerAndResetsTheGate() throws Exception {
        LockBoundaryGate timeoutGate = new LockBoundaryGate(2, TimeUnit.SECONDS);
        timeoutGate.arm(reservationId);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Object> waitingWorker = executor.submit(() -> intercept(timeoutGate));

        try {
            timeoutGate.awaitWaitingWorker();

            assertThatThrownBy(timeoutGate::awaitBothArrivals)
                    .hasMessageContaining("both workers must reach the repository lock boundary");

            assertThat(waitingWorker.get(1, TimeUnit.SECONDS)).isEqualTo(java.util.Optional.empty());
        } finally {
            cleanup(timeoutGate, executor, List.of(waitingWorker));
        }
        assertThat(timeoutGate.isArmed()).isFalse();
    }

    @Test
    void interruptedReleaseWaitRestoresTheWorkerInterruptFlag() throws Exception {
        LockBoundaryGate interruptedGate = new LockBoundaryGate(10, TimeUnit.SECONDS);
        interruptedGate.arm(reservationId);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch workerFinished = new CountDownLatch(1);
        AtomicBoolean interruptRestored = new AtomicBoolean();

        executor.submit(() -> {
            try {
                intercept(interruptedGate);
            } catch (InterruptedException exception) {
                interruptRestored.set(Thread.currentThread().isInterrupted());
            } catch (Throwable exception) {
                throw new AssertionError(exception);
            } finally {
                workerFinished.countDown();
            }
        });

        try {
            interruptedGate.awaitWaitingWorker();
            executor.shutdownNow();

            assertThat(workerFinished.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(interruptRestored).isTrue();
        } finally {
            cleanup(interruptedGate, executor, List.of());
        }
    }

    @Test
    void workerFailureDoesNotPreventFailureSafeCleanup() throws InterruptedException {
        ExecutorService executor = mock(ExecutorService.class);
        Future<ReservationResponse> failedWorker = failedFuture(new IllegalStateException("worker failed"));
        org.mockito.Mockito.when(executor.awaitTermination(10, TimeUnit.SECONDS)).thenReturn(true);
        lockBoundaryGate.arm(reservationId);

        assertThatThrownBy(() -> awaitResponse(failedWorker))
                .hasMessageContaining("Cancellation worker did not complete")
                .hasCauseInstanceOf(ExecutionException.class);

        cleanup(executor, List.of(failedWorker));
        assertThat(lockBoundaryGate.isArmed()).isFalse();
    }

    @Test
    void interruptedWorkerWaitRestoresTheInterruptFlag() {
        Future<ReservationResponse> interruptedWorker = interruptedFuture();

        try {
            assertThatThrownBy(() -> awaitResponse(interruptedWorker))
                    .hasMessageContaining("Interrupted while waiting for cancellation worker");
            assertThat(Thread.interrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void shutdownFailureStillResetsTheGate() {
        ExecutorService executor = mock(ExecutorService.class);
        doThrow(new IllegalStateException("shutdown failed")).when(executor).shutdownNow();
        lockBoundaryGate.arm(reservationId);

        assertThatThrownBy(() -> cleanup(executor, List.of()))
                .hasMessageContaining("shutdown failed");

        assertThat(lockBoundaryGate.isArmed()).isFalse();
    }

    @Test
    void shutdownExecutorFailsWhenWorkersDoNotTerminateWithinTheBound() throws InterruptedException {
        ExecutorService executor = mock(ExecutorService.class);
        org.mockito.Mockito.when(executor.awaitTermination(10, TimeUnit.SECONDS)).thenReturn(false);

        assertThatThrownBy(() -> shutdownExecutor(executor))
                .hasMessageContaining("Cancellation workers did not terminate");
    }

    private ReservationResponse awaitResponse(Future<ReservationResponse> future) {
        try {
            return future.get(20, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for cancellation worker", exception);
        } catch (Exception exception) {
            throw new AssertionError("Cancellation worker did not complete", exception);
        }
    }

    private void cleanup(ExecutorService executor, List<? extends Future<?>> calls) {
        cleanup(lockBoundaryGate, executor, calls);
    }

    private void cleanup(LockBoundaryGate gate, ExecutorService executor, List<? extends Future<?>> calls) {
        gate.release();
        calls.stream().filter(future -> !future.isDone()).forEach(future -> future.cancel(true));
        try {
            shutdownExecutor(executor);
        } finally {
            gate.reset();
        }
    }

    private Object intercept(LockBoundaryGate gate) throws Exception {
        try {
            return gate.intercept(lockInvocation());
        } catch (Exception exception) {
            throw exception;
        } catch (Throwable exception) {
            throw new AssertionError(exception);
        }
    }

    private org.aopalliance.intercept.MethodInvocation lockInvocation() throws NoSuchMethodException {
        org.aopalliance.intercept.MethodInvocation invocation = mock(org.aopalliance.intercept.MethodInvocation.class);
        org.mockito.Mockito.when(invocation.getMethod())
                .thenReturn(ReservationRepository.class.getMethod("findByIdForUpdate", Long.class));
        org.mockito.Mockito.when(invocation.getArguments()).thenReturn(new Object[]{reservationId});
        try {
            org.mockito.Mockito.when(invocation.proceed()).thenReturn(java.util.Optional.empty());
        } catch (Throwable exception) {
            throw new AssertionError(exception);
        }
        return invocation;
    }

    private void shutdownExecutor(ExecutorService executor) {
        boolean interrupted = false;
        executor.shutdownNow();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                throw new AssertionError("Cancellation workers did not terminate");
            }
        } catch (InterruptedException exception) {
            interrupted = true;
            throw new AssertionError("Interrupted while stopping cancellation workers", exception);
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static Future<ReservationResponse> failedFuture(Throwable cause) {
        return new Future<>() {
            @Override public boolean cancel(boolean mayInterruptIfRunning) { return false; }
            @Override public boolean isCancelled() { return false; }
            @Override public boolean isDone() { return true; }
            @Override public ReservationResponse get() throws ExecutionException { throw new ExecutionException(cause); }
            @Override public ReservationResponse get(long timeout, TimeUnit unit) throws ExecutionException {
                throw new ExecutionException(cause);
            }
        };
    }

    private static Future<ReservationResponse> interruptedFuture() {
        return new Future<>() {
            @Override public boolean cancel(boolean mayInterruptIfRunning) { return false; }
            @Override public boolean isCancelled() { return false; }
            @Override public boolean isDone() { return false; }
            @Override public ReservationResponse get() throws InterruptedException { throw new InterruptedException(); }
            @Override public ReservationResponse get(long timeout, TimeUnit unit) throws InterruptedException {
                throw new InterruptedException();
            }
        };
    }

    @TestConfiguration
    static class LockBoundaryTestConfiguration {
        @Bean
        LockBoundaryGate lockBoundaryGate() {
            return new LockBoundaryGate(10, TimeUnit.SECONDS);
        }

        @Bean(name = "lockBoundaryReservationRepository")
        @Primary
        ReservationRepository lockBoundaryReservationRepository(
                @Qualifier("reservationRepository") ReservationRepository delegate,
                LockBoundaryGate gate) {
            ProxyFactory proxyFactory = new ProxyFactory(delegate);
            proxyFactory.setInterfaces(ReservationRepository.class);
            proxyFactory.addAdvice((MethodInterceptor) gate::intercept);
            return (ReservationRepository) proxyFactory.getProxy();
        }
    }

    static final class LockBoundaryGate {
        private static final long RELEASE_WAIT_TIMEOUT_SECONDS = 10;

        private final long timeout;
        private final TimeUnit timeoutUnit;
        private volatile Long reservationId;
        private volatile CountDownLatch arrived;
        private volatile CountDownLatch waiting;
        private volatile CountDownLatch release;

        LockBoundaryGate(long timeout, TimeUnit timeoutUnit) {
            this.timeout = timeout;
            this.timeoutUnit = timeoutUnit;
        }

        synchronized void arm(Long id) {
            reservationId = id;
            arrived = new CountDownLatch(2);
            waiting = new CountDownLatch(1);
            release = new CountDownLatch(1);
        }

        Object intercept(org.aopalliance.intercept.MethodInvocation invocation) throws Throwable {
            Long armedReservationId = reservationId;
            if (armedReservationId != null
                     && invocation.getMethod().getName().equals("findByIdForUpdate")
                     && armedReservationId.equals(invocation.getArguments()[0])) {
                arrived.countDown();
                waiting.countDown();
                try {
                    if (!release.await(RELEASE_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                        throw new AssertionError("Timed out waiting to release repository lock boundary");
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw exception;
                }
            }
            return invocation.proceed();
        }

        void awaitBothArrivals() {
            try {
                boolean bothArrived = arrived.await(timeout, timeoutUnit);
                if (!bothArrived) {
                    release();
                }
                assertThat(bothArrived)
                    .as("both workers must reach the repository lock boundary")
                    .isTrue();
            } catch (InterruptedException exception) {
                release();
                Thread.currentThread().interrupt();
                throw new AssertionError("Interrupted while waiting for repository lock boundary", exception);
            }
        }

        void awaitWaitingWorker() {
            try {
                assertThat(waiting.await(timeout, timeoutUnit))
                        .as("a worker must wait at the repository lock boundary")
                        .isTrue();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new AssertionError("Interrupted while waiting for repository lock boundary", exception);
            }
        }

        void release() {
            release.countDown();
        }

        synchronized void reset() {
            reservationId = null;
            arrived = null;
            waiting = null;
            release = null;
        }

        synchronized boolean isArmed() {
            return reservationId != null;
        }
    }
}
