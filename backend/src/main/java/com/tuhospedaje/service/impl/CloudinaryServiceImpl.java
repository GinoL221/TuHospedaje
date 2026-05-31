package com.tuhospedaje.service.impl;

import com.cloudinary.Cloudinary;
import com.tuhospedaje.dto.upload.UploadResult;
import com.tuhospedaje.service.CloudinaryService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryServiceImpl(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @Override
    public UploadResult uploadImage(MultipartFile file) {
        try {
            Map raw = cloudinary.uploader().upload(file.getBytes(), Map.of());
            return new UploadResult(raw);
        } catch (Exception e) {
            throw new RuntimeException("Error al subir imagen a Cloudinary: " + e.getMessage());
        }
    }
}
