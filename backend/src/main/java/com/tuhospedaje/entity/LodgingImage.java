package com.tuhospedaje.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name="lodging_images")
public class LodgingImage {

    public static LodgingImage forLodging(Lodging lodging, String imageUrl) {
        LodgingImage image = new LodgingImage();
        image.setLodging(lodging);
        image.setImageUrl(imageUrl);
        return image;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "lodging_id", nullable = false)
    private Lodging lodging;

    @Column(nullable = false)
    private String imageUrl;

    private String title;
}
