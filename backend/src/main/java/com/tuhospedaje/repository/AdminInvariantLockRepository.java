package com.tuhospedaje.repository;

import com.tuhospedaje.entity.AdminInvariantLock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminInvariantLockRepository extends JpaRepository<AdminInvariantLock, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT lockRow FROM AdminInvariantLock lockRow WHERE lockRow.id = :id")
    Optional<AdminInvariantLock> lockById(@Param("id") Long id);
}
