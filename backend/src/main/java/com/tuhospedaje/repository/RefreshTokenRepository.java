package com.tuhospedaje.repository;

import com.tuhospedaje.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByHmacKeyIdAndTokenHmac(String hmacKeyId, byte[] tokenHmac);
}
