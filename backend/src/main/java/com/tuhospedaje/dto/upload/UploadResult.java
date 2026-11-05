package com.tuhospedaje.dto.upload;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "Result of a successful image upload")
public class UploadResult {

    @Schema(description = "URL of the stored image: the configured public upload prefix " +
            "(default /uploads, same origin) joined with the stored file name",
            example = "/uploads/3f2c9a1e-8c1d-4b7e-9f0a-2d6e5b8c7a1f.jpg")
    private final String url;

    @Schema(description = "Name of the stored file (identifier for future management operations)",
            example = "3f2c9a1e-8c1d-4b7e-9f0a-2d6e5b8c7a1f.jpg")
    private final String publicId;

    public UploadResult(String url, String publicId) {
        this.url = url;
        this.publicId = publicId;
    }
}
