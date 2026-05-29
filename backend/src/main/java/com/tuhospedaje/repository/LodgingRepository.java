package com.tuhospedaje.repository;

import com.tuhospedaje.entity.Lodging;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
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
}
