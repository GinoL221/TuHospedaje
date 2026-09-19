package com.tuhospedaje.upload;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.tuhospedaje.dto.upload.UploadResult;
import com.tuhospedaje.exception.UploadException;
import com.tuhospedaje.service.command.UploadImageCommand;
import com.tuhospedaje.service.impl.CloudinaryServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    void uploadImage_acceptsAnUploadImageCommand() throws Exception {
        Uploader uploader = successfulUploader();
        byte[] imageBytes = validImageBytes("image/jpeg");

        UploadResult result = cloudinaryService.uploadImage(command(imageBytes, "image/jpeg"));

        assertThat(result).isNotNull();
        verify(uploader).upload(imageBytes, Map.of());
    }

    @Test
    void uploadImage_whenCloudinaryThrows_throwsUploadException() throws Exception {
        Uploader uploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap())).thenThrow(new RuntimeException("Cloudinary connection failed"));

        assertThatThrownBy(() -> cloudinaryService.uploadImage(command(validImageBytes("image/png"), "image/png")))
                .isInstanceOf(UploadException.class);
    }

    @Test
    void uploadImage_whenContentIsEmpty_rejectsBeforeCallingCloudinary() {
        assertThatThrownBy(() -> cloudinaryService.uploadImage(command(new byte[0], "image/jpeg")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("error.upload.empty");

        verify(cloudinary, never()).uploader();
    }

    @ParameterizedTest
    @ValueSource(strings = {"application/pdf", "text/html", "image/svg+xml", "application/zip"})
    void uploadImage_whenContentTypeIsNotAnAllowedImage_rejectsBeforeCallingCloudinary(String contentType) throws Exception {
        assertThatThrownBy(() -> cloudinaryService.uploadImage(command(validImageBytes("image/jpeg"), contentType)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("error.upload.invalid_type");

        verify(cloudinary, never()).uploader();
    }

    @Test
    void uploadImage_whenContentTypeIsMissing_rejectsBeforeCallingCloudinary() throws Exception {
        assertThatThrownBy(() -> cloudinaryService.uploadImage(command(validImageBytes("image/jpeg"), null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("error.upload.invalid_type");

        verify(cloudinary, never()).uploader();
    }

    @ParameterizedTest
    @ValueSource(strings = {"image/jpeg", "image/png", "image/webp", "image/gif"})
    void uploadImage_acceptsTheSupportedRasterFormats(String contentType) throws Exception {
        successfulUploader();

        assertThat(cloudinaryService.uploadImage(command(validImageBytes(contentType), contentType))).isNotNull();
    }

    @Test
    void uploadImage_ignoresContentTypeParametersAndCasing() throws Exception {
        successfulUploader();

        assertThat(cloudinaryService.uploadImage(command(validImageBytes("image/jpeg"), "IMAGE/JPEG; charset=binary"))).isNotNull();
    }

    @Test
    void uploadImage_whenMimeDoesNotMatchBytes_rejectsBeforeCallingCloudinary() throws Exception {
        assertThatThrownBy(() -> cloudinaryService.uploadImage(command(validImageBytes("image/png"), "image/jpeg")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("error.upload.invalid_type");

        verify(cloudinary, never()).uploader();
    }

    @Test
    void uploadImage_whenImageBytesAreMalformed_rejectsBeforeCallingCloudinary() {
        assertThatThrownBy(() -> cloudinaryService.uploadImage(command(new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47}, "image/png")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("error.upload.invalid_type");

        verify(cloudinary, never()).uploader();
    }

    @Test
    void uploadImage_whenWebpRiffIsTruncated_rejectsBeforeCallingCloudinary() {
        assertThatThrownBy(() -> cloudinaryService.uploadImage(command(
                new byte[]{'R', 'I', 'F', 'F', 18, 0, 0, 0, 'W', 'E', 'B', 'P'}, "image/webp")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("error.upload.invalid_type");

        verify(cloudinary, never()).uploader();
    }

    @Test
    void uploadImage_whenPngPixelsExceedTheDecodedImageLimit_rejectsBeforeCallingCloudinary() throws Exception {
        byte[] imageBytes = validImageBytes("image/png");
        writeInt(imageBytes, 16, 5_000);
        writeInt(imageBytes, 20, 5_000);

        assertThatThrownBy(() -> cloudinaryService.uploadImage(command(imageBytes, "image/png")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("error.upload.invalid_type");

        verify(cloudinary, never()).uploader();
    }

    @Test
    void uploadImage_whenContentIsLargerThanFiveMiB_rejectsBeforeCallingCloudinary() {
        assertThatThrownBy(() -> cloudinaryService.uploadImage(command(new byte[5 * 1024 * 1024 + 1], "image/jpeg")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("error.upload.too_large");

        verify(cloudinary, never()).uploader();
    }

    private Uploader successfulUploader() {
        Uploader uploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        try {
            when(uploader.upload(any(byte[].class), anyMap())).thenReturn(Map.of("secure_url", "https://res.cloudinary.com/img"));
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        return uploader;
    }

    private UploadImageCommand command(byte[] content, String contentType) {
        return new UploadImageCommand(content, contentType);
    }

    private void writeInt(byte[] bytes, int offset, int value) {
        bytes[offset] = (byte) (value >>> 24);
        bytes[offset + 1] = (byte) (value >>> 16);
        bytes[offset + 2] = (byte) (value >>> 8);
        bytes[offset + 3] = (byte) value;
    }

    private byte[] validImageBytes(String contentType) throws Exception {
        if ("image/webp".equals(contentType)) {
            return new byte[]{'R', 'I', 'F', 'F', 18, 0, 0, 0, 'W', 'E', 'B', 'P', 'V', 'P', '8', 'L', 5, 0, 0, 0,
                    0x2F, 0, 0, 0, 0, 0};
        }
        String format = contentType.substring("image/".length());
        if ("jpeg".equals(format)) {
            format = "jpg";
        }
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (!ImageIO.write(image, format, output)) {
            throw new IllegalStateException("No ImageIO writer for " + format);
        }
        return output.toByteArray();
    }
}
