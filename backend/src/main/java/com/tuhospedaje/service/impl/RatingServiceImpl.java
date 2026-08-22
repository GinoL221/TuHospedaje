package com.tuhospedaje.service.impl;

import com.tuhospedaje.dto.rating.RatingDTO;
import com.tuhospedaje.dto.rating.RatingEligibilityDTO;
import com.tuhospedaje.entity.Lodging;
import com.tuhospedaje.entity.Rating;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.ReservationStatus;
import com.tuhospedaje.exception.ResourceNotFoundException;
import com.tuhospedaje.repository.LodgingRepository;
import com.tuhospedaje.repository.RatingRepository;
import com.tuhospedaje.repository.ReservationRepository;
import com.tuhospedaje.service.RatingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class RatingServiceImpl implements RatingService {

    private final RatingRepository ratingRepository;
    private final LodgingRepository lodgingRepository;
    private final ReservationRepository reservationRepository;
    private final Clock clock;

    public RatingServiceImpl(RatingRepository ratingRepository, LodgingRepository lodgingRepository,
                             ReservationRepository reservationRepository, Clock clock) {
        this.ratingRepository = ratingRepository;
        this.lodgingRepository = lodgingRepository;
        this.reservationRepository = reservationRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public RatingDTO createRating(User user, Long lodgingId, Integer score, String comment) {
        if (!isEligible(user.getId(), lodgingId)) {
            throw new IllegalArgumentException(
                    "Solo los huéspedes con una estadía confirmada y finalizada pueden puntuar este alojamiento");
        }

        Lodging lodging = lodgingRepository.findById(lodgingId)
                .orElseThrow(() -> new ResourceNotFoundException("Alojamiento no encontrado"));

        Optional<Rating> existingRatingOpt = ratingRepository.findByUserIdAndLodgingId(user.getId(), lodgingId);
        Rating rating;
        if (existingRatingOpt.isPresent()) {
            rating = existingRatingOpt.get();
            rating.setScore(score);
            rating.setComment(comment);
        } else {
            rating = new Rating();
            rating.setLodging(lodging);
            rating.setUser(user);
            rating.setScore(score);
            rating.setComment(comment);
        }
        rating.setCreatedAt(LocalDateTime.now(clock));

        return RatingDTO.fromEntity(ratingRepository.save(rating));
    }

    @Override
    @Transactional(readOnly = true)
    public RatingEligibilityDTO getEligibility(User user, Long lodgingId) {
        return isEligible(user.getId(), lodgingId)
                ? RatingEligibilityDTO.eligible()
                : RatingEligibilityDTO.ineligible();
    }

    private boolean isEligible(Long userId, Long lodgingId) {
        LocalDate businessToday = LocalDate.now(clock);
        return reservationRepository.existsByUserIdAndLodgingIdAndStatusAndCheckOutBefore(
                userId, lodgingId, ReservationStatus.CONFIRMED, businessToday);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getRatingsByLodging(Long lodgingId) {
        List<Rating> ratings = ratingRepository.findByLodgingIdOrderByCreatedAtDesc(lodgingId);
        double average = ratings.isEmpty() ? 0.0
                : ratings.stream().mapToInt(Rating::getScore).average().orElse(0.0);

        return Map.of(
                "average", Math.round(average * 10.0) / 10.0,
                "count", ratings.size(),
                "ratings", ratings.stream().map(RatingDTO::fromEntity).toList()
        );
    }
}
