package com.tuhospedaje.repository;

import com.tuhospedaje.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {

    Optional<Rating> findByUserIdAndLodgingId(Long userId, Long lodgingId);

    List<Rating> findByLodgingIdOrderByCreatedAtDesc(Long lodgingId);

    int countByLodgingId(Long lodgingId);
}
