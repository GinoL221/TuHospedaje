package com.tuhospedaje.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "admin_invariant_lock")
public class AdminInvariantLock {

    @Id
    private Long id;
}
