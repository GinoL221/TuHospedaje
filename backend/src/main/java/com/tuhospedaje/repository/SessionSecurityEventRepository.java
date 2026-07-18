package com.tuhospedaje.repository;

import com.tuhospedaje.entity.SessionSecurityEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SessionSecurityEventRepository extends JpaRepository<SessionSecurityEvent, Long> {
}
