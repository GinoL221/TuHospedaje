package com.tuhospedaje.repository;

import com.tuhospedaje.entity.Feature;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FeatureRepository extends JpaRepository<Feature, Long> {

    boolean existsByNameIgnoreCase(String name);

    @Nullable Feature findByNameIgnoreCase(String name);
}
