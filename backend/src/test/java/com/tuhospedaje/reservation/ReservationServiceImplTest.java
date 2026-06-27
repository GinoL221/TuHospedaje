package com.tuhospedaje.reservation;

import com.tuhospedaje.dto.reservation.CreateReservationRequest;
import com.tuhospedaje.dto.reservation.ReservationResponse;
import com.tuhospedaje.entity.Lodging;
import com.tuhospedaje.entity.Reservation;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.ReservationStatus;
import com.tuhospedaje.enums.RoleEnum;
import com.tuhospedaje.exception.ResourceNotFoundException;
import com.tuhospedaje.repository.LodgingRepository;
import com.tuhospedaje.repository.ReservationRepository;
import com.tuhospedaje.service.EmailService;
import com.tuhospedaje.service.impl.ReservationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceImplTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private LodgingRepository lodgingRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private ReservationServiceImpl reservationService;

    // --- createReservation ---

    @Test
    void createReservation_whenLodgingNotFound_throwsResourceNotFoundException() {
        User user = buildUser(1L, RoleEnum.USER);
        CreateReservationRequest request = buildRequest(999L);

        when(lodgingRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> reservationService.createReservation(user, request)
        );
        assertThat(ex.getMessage()).contains("Alojamiento no encontrado");
    }

    @Test
    void createReservation_whenDatesConflict_throwsIllegalArgumentException() {
        User user = buildUser(1L, RoleEnum.USER);
        Lodging lodging = buildLodging(10L, new BigDecimal("100.00"));
        CreateReservationRequest request = buildRequest(10L);

        when(lodgingRepository.findById(10L)).thenReturn(Optional.of(lodging));

        // Simulate an overlapping existing reservation
        Reservation existing = new Reservation();
        existing.setCheckIn(LocalDate.now().plusDays(5));
        existing.setCheckOut(LocalDate.now().plusDays(15));
        when(reservationRepository.lockByLodgingIdAndStatus(eq(10L), eq(ReservationStatus.CONFIRMED)))
                .thenReturn(List.of(existing));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> reservationService.createReservation(user, request)
        );
        assertThat(ex.getMessage()).contains("no está disponible");
    }

    @Test
    void createReservation_whenNoConflict_savesAndReturnsResponse() {
        User user = buildUser(1L, RoleEnum.USER);
        Lodging lodging = buildLodging(10L, new BigDecimal("150.00"));
        CreateReservationRequest request = buildRequest(10L);

        when(lodgingRepository.findById(10L)).thenReturn(Optional.of(lodging));
        when(reservationRepository.lockByLodgingIdAndStatus(eq(10L), eq(ReservationStatus.CONFIRMED)))
                .thenReturn(Collections.emptyList());

        Reservation saved = new Reservation();
        saved.setId(1L);
        saved.setLodging(lodging);
        saved.setUser(user);
        saved.setCheckIn(request.getCheckIn());
        saved.setCheckOut(request.getCheckOut());
        saved.setGuestName(request.getGuestName());
        saved.setGuestEmail(request.getGuestEmail());
        saved.setGuestPhone(request.getGuestPhone());
        saved.setTotalPrice(new BigDecimal("300.00"));
        saved.setStatus(ReservationStatus.CONFIRMED);
        when(reservationRepository.save(any(Reservation.class))).thenReturn(saved);

        var response = reservationService.createReservation(user, request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
    }

    // --- getMyReservations ---

    @Test
    void getMyReservations_returnsReservationsMappedToResponse() {
        User user = buildUser(1L, RoleEnum.USER);
        Lodging lodging = buildLodging(10L, new BigDecimal("100.00"));

        Reservation r = new Reservation();
        r.setId(1L);
        r.setLodging(lodging);
        r.setUser(user);
        r.setCheckIn(LocalDate.now().plusDays(20));
        r.setCheckOut(LocalDate.now().plusDays(22));
        r.setGuestName("Test Guest");
        r.setGuestEmail("guest@test.com");
        r.setGuestPhone("111222333");
        r.setTotalPrice(new BigDecimal("200.00"));
        r.setStatus(ReservationStatus.CONFIRMED);

        when(reservationRepository.findByUserIdOrderByCheckInDesc(1L)).thenReturn(List.of(r));

        List<ReservationResponse> result = reservationService.getMyReservations(user);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getGuestEmail()).isEqualTo("guest@test.com");
    }

    @Test
    void getMyReservations_returnsEmptyListWhenNoReservationsExist() {
        User user = buildUser(1L, RoleEnum.USER);

        when(reservationRepository.findByUserIdOrderByCheckInDesc(1L)).thenReturn(Collections.emptyList());

        List<ReservationResponse> result = reservationService.getMyReservations(user);

        assertThat(result).isEmpty();
    }

    // --- getReservationById ---

    @Test
    void getReservationById_whenNotFound_throwsResourceNotFoundException() {
        User requester = buildUser(1L, RoleEnum.USER);

        when(reservationRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> reservationService.getReservationById(999L, requester)
        );
        assertThat(ex.getMessage()).contains("999");
    }

    @Test
    void getReservationById_whenNonOwnerNonAdmin_throwsResourceNotFoundException() {
        User owner = buildUser(1L, RoleEnum.USER);
        User otherUser = buildUser(2L, RoleEnum.USER);

        Lodging lodging = buildLodging(10L, new BigDecimal("100.00"));
        Reservation reservation = new Reservation();
        reservation.setId(42L);
        reservation.setLodging(lodging);
        reservation.setUser(owner);
        reservation.setCheckIn(LocalDate.now().plusDays(5));
        reservation.setCheckOut(LocalDate.now().plusDays(7));
        reservation.setGuestName("Owner Guest");
        reservation.setGuestEmail("owner@test.com");
        reservation.setGuestPhone("111222333");
        reservation.setTotalPrice(new BigDecimal("200.00"));
        reservation.setStatus(ReservationStatus.CONFIRMED);

        when(reservationRepository.findById(42L)).thenReturn(Optional.of(reservation));

        // Non-owner, non-admin → ResourceNotFoundException (IDOR prevention)
        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> reservationService.getReservationById(42L, otherUser)
        );
        assertThat(ex.getMessage()).contains("42");
    }

    @Test
    void getReservationById_whenAdmin_returnsReservationRegardlessOfOwnership() {
        User owner = buildUser(1L, RoleEnum.USER);
        User admin = buildUser(99L, RoleEnum.ADMIN);

        Lodging lodging = buildLodging(10L, new BigDecimal("100.00"));
        Reservation reservation = new Reservation();
        reservation.setId(42L);
        reservation.setLodging(lodging);
        reservation.setUser(owner);
        reservation.setCheckIn(LocalDate.now().plusDays(5));
        reservation.setCheckOut(LocalDate.now().plusDays(7));
        reservation.setGuestName("Owner Guest");
        reservation.setGuestEmail("owner@test.com");
        reservation.setGuestPhone("111222333");
        reservation.setTotalPrice(new BigDecimal("200.00"));
        reservation.setStatus(ReservationStatus.CONFIRMED);

        when(reservationRepository.findById(42L)).thenReturn(Optional.of(reservation));

        var response = reservationService.getReservationById(42L, admin);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(42L);
    }

    // --- helpers ---

    private static User buildUser(Long id, RoleEnum role) {
        User user = new User();
        user.setId(id);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail("test" + id + "@tuhospedaje.com");
        user.setPassword("secret");
        user.setRole(role);
        return user;
    }

    private static Lodging buildLodging(Long id, BigDecimal pricePerNight) {
        Lodging lodging = new Lodging();
        lodging.setId(id);
        lodging.setName("Test Lodging");
        lodging.setDescription("desc");
        lodging.setAddress("Calle 1");
        lodging.setCity("Ciudad");
        lodging.setCountry("Pais");
        lodging.setPhoneNumber("111222333");
        lodging.setEmail("lodging@test.com");
        lodging.setPricePerNight(pricePerNight);
        lodging.setMaxGuests(4);
        return lodging;
    }

    private static CreateReservationRequest buildRequest(Long lodgingId) {
        CreateReservationRequest req = new CreateReservationRequest();
        req.setLodgingId(lodgingId);
        req.setCheckIn(LocalDate.now().plusDays(10));
        req.setCheckOut(LocalDate.now().plusDays(12));
        req.setGuestName("Test Guest");
        req.setGuestEmail("guest@test.com");
        req.setGuestPhone("111222333");
        return req;
    }
}
