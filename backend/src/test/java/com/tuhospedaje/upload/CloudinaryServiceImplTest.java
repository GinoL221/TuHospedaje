package com.tuhospedaje.upload;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.tuhospedaje.dto.upload.UploadResult;
import com.tuhospedaje.exception.UploadException;
import com.tuhospedaje.service.impl.CloudinaryServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CloudinaryServiceImplTest {

    @Mock
    private Cloudinary cloudinary;

    @InjectMocks
    private CloudinaryServiceImpl cloudinaryService;

    @Test
    void uploadImage_whenCloudinarySucceeds_returnsUploadResult() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        Uploader uploader = mock(Uploader.class);

        when(file.getContentType()).thenReturn("image/jpeg");
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

        when(file.getContentType()).thenReturn("image/png");
        when(file.getBytes()).thenReturn(new byte[]{1});
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap()))
                .thenThrow(new RuntimeException("Cloudinary connection failed"));

        assertThrows(UploadException.class, () -> cloudinaryService.uploadImage(file));
    }

    /**
     * An empty part is rejected before any network call. Without this guard the
     * request travels to Cloudinary only to come back as a 502, which reads to the
     * client as "our image provider is down" rather than "you sent nothing".
     */
    @Test
    void uploadImage_whenFileIsEmpty_rejectsBeforeCallingCloudinary() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);

        assertThatThrownBy(() -> cloudinaryService.uploadImage(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("error.upload.empty");

        verify(cloudinary, never()).uploader();
    }

    /**
     * The message is a MessageSource key, not prose: GlobalExceptionHandler resolves
     * IllegalArgumentException messages as keys and falls back to the literal only
     * when the key is unregistered.
     */
    @ParameterizedTest
    @ValueSource(strings = {"application/pdf", "text/html", "image/svg+xml", "application/zip"})
    void uploadImage_whenContentTypeIsNotAnAllowedImage_rejectsBeforeCallingCloudinary(String contentType) {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getContentType()).thenReturn(contentType);

        assertThatThrownBy(() -> cloudinaryService.uploadImage(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("error.upload.invalid_type");

        verify(cloudinary, never()).uploader();
    }

    /** A part with no Content-Type header is unverifiable, so it is refused rather than guessed. */
    @Test
    void uploadImage_whenContentTypeIsMissing_rejectsBeforeCallingCloudinary() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getContentType()).thenReturn(null);

        assertThatThrownBy(() -> cloudinaryService.uploadImage(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("error.upload.invalid_type");

        verify(cloudinary, never()).uploader();
    }

    @ParameterizedTest
    @ValueSource(strings = {"image/jpeg", "image/png", "image/webp", "image/gif"})
    void uploadImage_acceptsTheSupportedRasterFormats(String contentType) throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        Uploader uploader = mock(Uploader.class);

        when(file.getContentType()).thenReturn(contentType);
        when(file.getBytes()).thenReturn(new byte[]{1, 2, 3});
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(Map.of("secure_url", "https://res.cloudinary.com/img"));

        assertThat(cloudinaryService.uploadImage(file)).isNotNull();
    }

    /** Content-Type carries charset/boundary parameters in the wild; only the media type decides. */
    @Test
    void uploadImage_ignoresContentTypeParametersAndCasing() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        Uploader uploader = mock(Uploader.class);

        when(file.getContentType()).thenReturn("IMAGE/JPEG; charset=binary");
        when(file.getBytes()).thenReturn(new byte[]{1, 2, 3});
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(Map.of("secure_url", "https://res.cloudinary.com/img"));

        assertThat(cloudinaryService.uploadImage(file)).isNotNull();
    }
}
