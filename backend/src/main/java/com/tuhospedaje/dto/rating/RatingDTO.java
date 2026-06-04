package com.tuhospedaje.dto.rating;

import com.tuhospedaje.entity.Rating;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class RatingDTO {
    private Long id;
    private Long lodgingId;
    private String userName;
    private Integer score;
    private String comment;
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
