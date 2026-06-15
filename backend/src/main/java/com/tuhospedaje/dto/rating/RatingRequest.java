package com.tuhospedaje.dto.rating;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Payload to submit or update a lodging rating")
public class RatingRequest {

    @NotNull
    @Schema(description = "ID of the lodging to rate", example = "42")
    Long lodgingId;

    @NotNull
    @Min(1)
    @Max(5)
    @Schema(description = "Score from 1 (lowest) to 5 (highest)", example = "4", minimum = "1", maximum = "5")
    Integer score;

    @Schema(description = "Optional comment accompanying the score", example = "Great location and very clean rooms.")
    String comment;
}
