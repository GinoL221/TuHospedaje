package com.tuhospedaje.dto.reservation;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Schema(description = "Availability information for a lodging in a given date range")
public class AvailabilityResponse {

    @Schema(description = "True if the lodging has no confirmed reservations overlapping the requested dates", example = "true")
    private boolean available;

    @Schema(description = "List of confirmed date ranges that are already occupied")
    private List<OccupiedRange> occupiedRanges;
}
