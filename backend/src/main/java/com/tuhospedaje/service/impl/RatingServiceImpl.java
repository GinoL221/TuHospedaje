package com.tuhospedaje.service.impl;

import com.tuhospedaje.dto.rating.RatingDTO;
import com.tuhospedaje.entity.Lodging;
import com.tuhospedaje.entity.Rating;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.exception.ResourceNotFoundException;
import com.tuhospedaje.repository.LodgingRepository;
import com.tuhospedaje.repository.RatingRepository;
import com.tuhospedaje.service.RatingService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class RatingServiceImpl implements RatingService {

    private final RatingRepository ratingRepository;
    private final LodgingRepository lodgingRepository;

    public RatingServiceImpl(RatingRepository ratingRepository, LodgingRepository lodgingRepository) {
        this.ratingRepository = ratingRepository;
        this.lodgingRepository = lodgingRepository;
    }

    @Override
    public RatingDTO createRating(User user, Long lodgingId, Integer score, String comment) {
        Lodging lodging = lodgingRepository.findById(lodgingId)
                .orElseThrow(() -> new ResourceNotFoundException("Alojamiento no encontrado"));

        Rating rating = new Rating();
        rating.setLodging(lodging);
        rating.setUser(user);
        rating.setScore(score);
        rating.setComment(comment);

        return RatingDTO.fromEntity(ratingRepository.save(rating));
    }

    @Override
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
