package com.tuhospedaje.upload;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.tuhospedaje.dto.upload.UploadResult;
import com.tuhospedaje.exception.UploadException;
import com.tuhospedaje.service.impl.CloudinaryServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CloudinaryServiceImplTest {

    @Mock
    private Cloudinary cloudinary;

    @InjectMocks
    private CloudinaryServiceImpl cloudinaryService;

    @Test
    @SuppressWarnings("unchecked")
    void uploadImage_whenCloudinarySucceeds_returnsUploadResult() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        Uploader uploader = mock(Uploader.class);

        when(file.getBytes()).thenReturn(new byte[]{1, 2, 3});
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(Map.of("secure_url", "https://res.cloudinary.com/img.jpg"));

        UploadResult result = cloudinaryService.uploadImage(file);

        assertThat(result).isNotNull();
    }

    @Test
    void uploadImage_whenCloudinaryThrows_throwsUploadException() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        Uploader uploader = mock(Uploader.class);

        when(file.getBytes()).thenReturn(new byte[]{1});
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap()))
                .thenThrow(new RuntimeException("Cloudinary connection failed"));

        assertThrows(UploadException.class, () -> cloudinaryService.uploadImage(file));
    }
}
