package com.tuhospedaje.repository;

import com.tuhospedaje.entity.Lodging;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LodgingRepository extends JpaRepository<Lodging, Long>, JpaSpecificationExecutor<Lodging> {

    Optional<Lodging> findByName(String name);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    List<Lodging> findByNameContainingIgnoreCase(String name);

    List<Lodging> findByCityIgnoreCase(String city);

    List<Lodging> findByCountryIgnoreCase(String country);

    List<Lodging> findByNameContainingIgnoreCaseAndCityIgnoreCase(String name, String city);

    boolean existsByName(String name);

    long countByCategoryId(Long categoryId);

    List<Lodging> findByCategoryId(Long categoryId);

    List<CityProjection> findDistinctByCityContainingIgnoreCaseOrderByCityAsc(String city);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM Lodging l WHERE l.id = :id")
    Optional<Lodging> findByIdForUpdate(@Param("id") Long id);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(
            value = "DELETE FROM user_favorites WHERE lodging_id = :lodgingId",
            nativeQuery = true)
    void deleteFavoritesByLodgingId(@org.springframework.data.repository.query.Param("lodgingId") Long lodgingId);
}
