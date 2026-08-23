package com.tuhospedaje.rating;

import com.tuhospedaje.dto.rating.RatingEligibilityDTO;
import com.tuhospedaje.entity.Lodging;
import com.tuhospedaje.entity.Rating;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.ReservationStatus;
import com.tuhospedaje.enums.RoleEnum;
import com.tuhospedaje.repository.LodgingRepository;
import com.tuhospedaje.repository.RatingRepository;
import com.tuhospedaje.repository.ReservationRepository;
import com.tuhospedaje.service.impl.RatingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** Unit coverage for the US-28.1 eligibility rule, evaluated through an injected {@link Clock}. */
@ExtendWith(MockitoExtension.class)
class RatingEligibilityServiceTest {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-08-15T12:00:00Z"), BUSINESS_ZONE);

    @Mock private RatingRepository ratingRepository;
    @Mock private LodgingRepository lodgingRepository;
    @Mock private ReservationRepository reservationRepository;

    private RatingServiceImpl ratingService;

    @BeforeEach
    void setUpService() {
        ratingService = new RatingServiceImpl(ratingRepository, lodgingRepository, reservationRepository, FIXED_CLOCK);
    }

    // S1: eligible completed stay
    @Test
    void eligibleCompletedStay_returnsEligibleTrue() {
        stubExists(true);

        RatingEligibilityDTO result = ratingService.getEligibility(user(1L), 10L);

        assertThat(result.isEligible()).isTrue();
        assertThat(result.getReason()).isEqualTo("ELIGIBLE");
    }

    // S2/S3/S4/S6 collapse to this branch at service level; boundary-day and cancelled
    // exclusion are additionally proven against a real database in the integration test.
    @Test
    void noQualifyingReservation_returnsIneligibleWithReason() {
        stubExists(false);

        RatingEligibilityDTO result = ratingService.getEligibility(user(1L), 10L);

        assertThat(result.isEligible()).isFalse();
        assertThat(result.getReason()).isEqualTo("COMPLETED_STAY_REQUIRED");
    }

    // S7: deterministic clock
    @Test
    void deterministicClock_repeatedEvaluationReturnsSameResult() {
        stubExists(true);

        RatingEligibilityDTO first = ratingService.getEligibility(user(1L), 10L);
        RatingEligibilityDTO second = ratingService.getEligibility(user(1L), 10L);

        assertThat(first.isEligible()).isEqualTo(second.isEligible());
        assertThat(first.getReason()).isEqualTo(second.getReason());
    }

    // rejected ineligible POST (create/update)
    @Test
    void createRating_whenIneligible_throwsWithPreciseMessage() {
        stubExists(false);

        assertThatThrownBy(() -> ratingService.createRating(user(1L), 10L, 5, "great"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("confirmada")
                .hasMessageContaining("finalizada");
    }

    @Test
    void createRating_whenEligible_setsCreatedAtFromInjectedClock() {
        stubExists(true);
        Lodging lodging = new Lodging();
        lodging.setId(10L);
        when(lodgingRepository.findById(10L)).thenReturn(Optional.of(lodging));
        when(ratingRepository.findByUserIdAndLodgingId(1L, 10L)).thenReturn(Optional.empty());
        when(ratingRepository.save(any(Rating.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = ratingService.createRating(user(1L), 10L, 5, "great");

        assertThat(result.getCreatedAt()).isEqualTo(LocalDateTime.now(FIXED_CLOCK));
    }

    private void stubExists(boolean value) {
        when(reservationRepository.existsByUserIdAndLodgingIdAndStatusAndCheckOutBefore(
                1L, 10L, ReservationStatus.CONFIRMED, LocalDate.now(FIXED_CLOCK)))
                .thenReturn(value);
    }

    private static User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setRole(RoleEnum.USER);
        return user;
    }
}
