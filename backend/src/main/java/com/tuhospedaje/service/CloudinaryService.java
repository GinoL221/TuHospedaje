package com.tuhospedaje.service;

import com.tuhospedaje.dto.upload.UploadResult;
import org.springframework.web.multipart.MultipartFile;

public interface CloudinaryService {
    UploadResult uploadImage(MultipartFile file);
}
