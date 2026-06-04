package com.tuhospedaje.dto.rating;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RatingRequest {
    @NotNull Long lodgingId;
    @NotNull @Min(1) @Max(5) Integer score;
    String comment;
}
