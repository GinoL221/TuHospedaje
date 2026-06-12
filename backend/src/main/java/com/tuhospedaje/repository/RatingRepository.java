package com.tuhospedaje.repository;

import com.tuhospedaje.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {

    Optional<Rating> findByUserIdAndLodgingId(Long userId, Long lodgingId);

    List<Rating> findByLodgingIdOrderByCreatedAtDesc(Long lodgingId);

    int countByLodgingId(Long lodgingId);

    /**
     * Single aggregate query: returns AVG score and COUNT for each lodging in the given id set.
     * Lodgings with no ratings are absent from the result — callers must post-fill with 0.0/0.
     * Empty {@code ids} collection is short-circuited before hitting the DB (some DBs reject empty IN).
     */
    @Query("SELECT r.lodging.id AS lodgingId, AVG(r.score) AS average, COUNT(r) AS count " +
           "FROM Rating r WHERE r.lodging.id IN :ids GROUP BY r.lodging.id")
    List<RatingAggregate> aggregateByLodgingIdsQuery(@Param("ids") Collection<Long> ids);

    default List<RatingAggregate> aggregateByLodgingIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return aggregateByLodgingIdsQuery(ids);
    }

    interface RatingAggregate {
        Long getLodgingId();
        Double getAverage();
        Long getCount();
    }
}
