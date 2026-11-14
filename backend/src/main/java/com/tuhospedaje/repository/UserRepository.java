package com.tuhospedaje.repository;

import com.tuhospedaje.entity.User;
import jakarta.persistence.LockModeType;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> lockByEmail(String email);

    @Modifying
    @Query(value = "UPDATE users SET welcome_resend_requested_at = :requestedAt WHERE id = :id", nativeQuery = true)
    int updateWelcomeResendRequestedAt(@Param("id") Long id, @Param("requestedAt") @Nullable Instant requestedAt);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.role = com.tuhospedaje.enums.RoleEnum.ADMIN AND u.enabled = true ORDER BY u.id ASC")
    List<User> lockEnabledAdminsInIdOrder();
}
