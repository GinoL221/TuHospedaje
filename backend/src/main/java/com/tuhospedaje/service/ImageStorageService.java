package com.tuhospedaje.service;

import com.tuhospedaje.dto.upload.UploadResult;
import com.tuhospedaje.service.command.UploadImageCommand;

public interface ImageStorageService {

    /**
     * Validates and stores the image, returning a result whose {@code url} points
     * at the publicly served path ({@code /uploads/<name>}) and whose
     * {@code publicId} is the stored file name.
     */
    UploadResult store(UploadImageCommand command);
}
