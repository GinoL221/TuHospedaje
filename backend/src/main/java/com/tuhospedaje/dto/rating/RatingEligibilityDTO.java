package com.tuhospedaje.dto.rating;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/** Result of the US-28.1 rating eligibility check. */
@Getter
@Setter
@Schema(description = "Whether the current user may submit a rating for a lodging")
public class RatingEligibilityDTO {

    @Schema(description = "Whether the user has a completed CONFIRMED stay for this lodging", example = "true")
    private boolean eligible;

    @Schema(description = "Machine-readable eligibility reason", example = "ELIGIBLE")
    private String reason;

    public static RatingEligibilityDTO eligible() {
        RatingEligibilityDTO dto = new RatingEligibilityDTO();
        dto.setEligible(true);
        dto.setReason("ELIGIBLE");
        return dto;
    }

    public static RatingEligibilityDTO ineligible() {
        RatingEligibilityDTO dto = new RatingEligibilityDTO();
        dto.setEligible(false);
        dto.setReason("COMPLETED_STAY_REQUIRED");
        return dto;
    }
}
