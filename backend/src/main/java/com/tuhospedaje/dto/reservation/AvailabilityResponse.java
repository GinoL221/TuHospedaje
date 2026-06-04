package com.tuhospedaje.dto.reservation;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AvailabilityResponse {

    private boolean available;
    private List<OccupiedRange> occupiedRanges;
}
