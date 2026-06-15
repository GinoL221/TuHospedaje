package com.tuhospedaje.dto.upload;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.Map;

@Getter
@Schema(description = "Result of a successful image upload to Cloudinary")
public class UploadResult {

    @Schema(description = "Public URL of the uploaded image", example = "https://res.cloudinary.com/demo/image/upload/v1234567890/sample.jpg")
    private final String url;

    @Schema(description = "Cloudinary asset identifier used for future management operations", example = "hoteleria/lodgings/sample")
    private final String publicId;

    @SuppressWarnings("unchecked")
    public UploadResult(Map<?, ?> raw) {
        this.url = (String) raw.get("url");
        this.publicId = (String) raw.get("public_id");
    }
}
