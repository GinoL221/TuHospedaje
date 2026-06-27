package com.tuhospedaje.dto.reservation;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Schema(description = "A date range already occupied by a confirmed reservation")
public class OccupiedRange {

    @Schema(description = "Start of the occupied period (inclusive)", example = "2025-07-15")
    private LocalDate checkIn;

    @Schema(description = "End of the occupied period (exclusive)", example = "2025-07-20")
    private LocalDate checkOut;
}
