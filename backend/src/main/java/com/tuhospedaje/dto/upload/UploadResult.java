package com.tuhospedaje.dto.upload;

import lombok.Getter;

import java.util.Map;

@Getter
public class UploadResult {

    private final String url;
    private final String publicId;

    @SuppressWarnings("unchecked")
    public UploadResult(Map<?, ?> raw) {
        this.url = (String) raw.get("url");
        this.publicId = (String) raw.get("public_id");
    }
}
