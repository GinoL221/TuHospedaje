package com.tuhospedaje.dto.reservation;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class OccupiedRange {

    private LocalDate checkIn;
    private LocalDate checkOut;
}
