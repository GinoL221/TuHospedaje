package com.tuhospedaje.service;

import com.tuhospedaje.dto.upload.UploadResult;
import com.tuhospedaje.service.command.UploadImageCommand;

public interface CloudinaryService {
    UploadResult uploadImage(UploadImageCommand command);
}
