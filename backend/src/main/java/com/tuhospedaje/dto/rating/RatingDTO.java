package com.tuhospedaje.dto.rating;

import com.tuhospedaje.entity.Rating;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Schema(description = "Rating details returned after submission or retrieval")
public class RatingDTO {

    @Schema(description = "Unique rating ID", example = "15")
    private Long id;

    @Schema(description = "ID of the rated lodging", example = "42")
    private Long lodgingId;

    @Schema(description = "Full name of the user who submitted the rating", example = "Maria Gomez")
    private String userName;

    @Schema(description = "Score from 1 (lowest) to 5 (highest)", example = "4")
    private Integer score;

    @Schema(description = "Optional comment left by the user", example = "Great location and very clean rooms.")
    private String comment;

    @Schema(description = "Timestamp when the rating was created or last updated", example = "2025-07-21T10:30:00")
    private LocalDateTime createdAt;

    public static RatingDTO fromEntity(Rating rating) {
        RatingDTO dto = new RatingDTO();
        dto.setId(rating.getId());
        dto.setLodgingId(rating.getLodging().getId());
        dto.setUserName(rating.getUser().getFirstName() + " " + rating.getUser().getLastName());
        dto.setScore(rating.getScore());
        dto.setComment(rating.getComment());
        dto.setCreatedAt(rating.getCreatedAt());
        return dto;
    }
}
