package com.tuhospedaje.repository;

import com.tuhospedaje.entity.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, Long> {
    boolean existsByName(String name);
}
