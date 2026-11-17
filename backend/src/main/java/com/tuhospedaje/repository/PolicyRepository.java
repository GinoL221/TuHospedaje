package com.tuhospedaje.repository;

import com.tuhospedaje.entity.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, Long> {

    boolean existsByName(String name);

    Optional<Policy> findByNameIgnoreCase(String name);
}
