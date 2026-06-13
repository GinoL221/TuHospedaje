package com.tuhospedaje.rating;

import com.tuhospedaje.dto.rating.RatingDTO;
import com.tuhospedaje.entity.Lodging;
import com.tuhospedaje.entity.Rating;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.ReservationStatus;
import com.tuhospedaje.enums.RoleEnum;
import com.tuhospedaje.exception.ResourceNotFoundException;
import com.tuhospedaje.repository.LodgingRepository;
import com.tuhospedaje.repository.RatingRepository;
import com.tuhospedaje.repository.ReservationRepository;
import com.tuhospedaje.service.impl.RatingServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RatingServiceImplTest {

    @Mock
    private RatingRepository ratingRepository;

    @Mock
    private LodgingRepository lodgingRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private RatingServiceImpl ratingService;

    // --- createRating ---

    @Test
    void createRating_whenNoConfirmedReservation_throwsIllegalArgumentException() {
        User user = buildUser(1L);
        when(reservationRepository.existsByUserIdAndLodgingIdAndStatus(
                1L, 10L, ReservationStatus.CONFIRMED)).thenReturn(false);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> ratingService.createRating(user, 10L, 4, "Great stay")
        );
        assertThat(ex.getMessage()).contains("reserva confirmada");
        verify(lodgingRepository, never()).findById(any());
    }

    @Test
    void createRating_whenLodgingNotFound_throwsResourceNotFoundException() {
        User user = buildUser(1L);
        when(reservationRepository.existsByUserIdAndLodgingIdAndStatus(
                1L, 99L, ReservationStatus.CONFIRMED)).thenReturn(true);
        when(lodgingRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> ratingService.createRating(user, 99L, 3, "comment")
        );
        assertThat(ex.getMessage()).contains("Alojamiento no encontrado");
    }

    @Test
    void createRating_whenNoExistingRating_createsNewRating() {
        User user = buildUser(1L);
        Lodging lodging = buildLodging(10L);

        when(reservationRepository.existsByUserIdAndLodgingIdAndStatus(
                1L, 10L, ReservationStatus.CONFIRMED)).thenReturn(true);
        when(lodgingRepository.findById(10L)).thenReturn(Optional.of(lodging));
        when(ratingRepository.findByUserIdAndLodgingId(1L, 10L)).thenReturn(Optional.empty());

        Rating saved = buildRating(1L, lodging, user, 4, "Nice");
        when(ratingRepository.save(any(Rating.class))).thenReturn(saved);

        RatingDTO result = ratingService.createRating(user, 10L, 4, "Nice");

        assertThat(result).isNotNull();
        assertThat(result.getScore()).isEqualTo(4);
        assertThat(result.getComment()).isEqualTo("Nice");
    }

    @Test
    void createRating_whenExistingRating_updatesExistingRating() {
        User user = buildUser(1L);
        Lodging lodging = buildLodging(10L);

        when(reservationRepository.existsByUserIdAndLodgingIdAndStatus(
                1L, 10L, ReservationStatus.CONFIRMED)).thenReturn(true);
        when(lodgingRepository.findById(10L)).thenReturn(Optional.of(lodging));

        Rating existing = buildRating(1L, lodging, user, 2, "Old comment");
        when(ratingRepository.findByUserIdAndLodgingId(1L, 10L)).thenReturn(Optional.of(existing));

        Rating updated = buildRating(1L, lodging, user, 5, "Updated comment");
        when(ratingRepository.save(existing)).thenReturn(updated);

        RatingDTO result = ratingService.createRating(user, 10L, 5, "Updated comment");

        assertThat(result.getScore()).isEqualTo(5);
        assertThat(result.getComment()).isEqualTo("Updated comment");
    }

    // --- getRatingsByLodging ---

    @Test
    void getRatingsByLodging_whenNoRatings_returnsZeroAverageAndEmptyList() {
        when(ratingRepository.findByLodgingIdOrderByCreatedAtDesc(99L))
                .thenReturn(Collections.emptyList());

        Map<String, Object> result = ratingService.getRatingsByLodging(99L);

        assertThat(result.get("average")).isEqualTo(0.0);
        assertThat(result.get("count")).isEqualTo(0);
        assertThat((List<?>) result.get("ratings")).isEmpty();
    }

    @Test
    void getRatingsByLodging_whenRatingsExist_returnsCorrectAverage() {
        Lodging lodging = buildLodging(10L);
        User user1 = buildUser(1L);
        User user2 = buildUser(2L);

        Rating r1 = buildRating(1L, lodging, user1, 4, "Good");
        Rating r2 = buildRating(2L, lodging, user2, 2, "Average");

        when(ratingRepository.findByLodgingIdOrderByCreatedAtDesc(10L))
                .thenReturn(List.of(r1, r2));

        Map<String, Object> result = ratingService.getRatingsByLodging(10L);

        // average of 4 and 2 is 3.0
        assertThat(result.get("average")).isEqualTo(3.0);
        assertThat(result.get("count")).isEqualTo(2);
        assertThat((List<?>) result.get("ratings")).hasSize(2);
    }

    // --- helpers ---

    private static User buildUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setFirstName("Test");
        user.setLastName("User" + id);
        user.setEmail("user" + id + "@test.com");
        user.setPassword("secret");
        user.setRole(RoleEnum.USER);
        return user;
    }

    private static Lodging buildLodging(Long id) {
        Lodging lodging = new Lodging();
        lodging.setId(id);
        lodging.setName("Test Lodging " + id);
        lodging.setAddress("Calle 1");
        lodging.setCity("Ciudad");
        lodging.setCountry("Pais");
        lodging.setPhoneNumber("111222333");
        lodging.setEmail("lodging" + id + "@test.com");
        lodging.setPricePerNight(new BigDecimal("100.00"));
        lodging.setMaxGuests(4);
        return lodging;
    }

    private static Rating buildRating(Long id, Lodging lodging, User user, int score, String comment) {
        Rating rating = new Rating();
        rating.setId(id);
        rating.setLodging(lodging);
        rating.setUser(user);
        rating.setScore(score);
        rating.setComment(comment);
        rating.setCreatedAt(LocalDateTime.now());
        return rating;
    }
}
