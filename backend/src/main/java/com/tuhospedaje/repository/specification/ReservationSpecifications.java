package com.tuhospedaje.repository.specification;

import com.tuhospedaje.entity.Reservation;
import com.tuhospedaje.enums.ReservationStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class ReservationSpecifications {

    private ReservationSpecifications() {
        // Utility class - prevent instantiation
    }

    public static Specification<Reservation> withStatus(ReservationStatus status) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("status"), status);
    }

    public static Specification<Reservation> withSearchQuery(String q) {
        if (q == null || q.trim().isEmpty()) {
            return null;
        }
        
        String searchTerm = "%" + q.trim().toLowerCase() + "%";
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("guestName")), searchTerm));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("guestEmail")), searchTerm));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("guestPhone")), searchTerm));
            return criteriaBuilder.or(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Reservation> withStatusAndSearchQuery(ReservationStatus status, String q) {
        Specification<Reservation> statusSpec = withStatus(status);
        Specification<Reservation> searchSpec = withSearchQuery(q);
        
        if (searchSpec == null) {
            return statusSpec;
        }
        return statusSpec.and(searchSpec);
    }
}