package com.tuhospedaje.service;

import com.tuhospedaje.service.command.UploadImageCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Validation contract moved verbatim from the former upload service: same error
 * message keys, same constants, same content-type normalization.
 */
class ImageContentValidatorTest {

    private final ImageContentValidator validator = new ImageContentValidator();

    @Test
    void validate_acceptsAnUploadImageCommand() throws Exception {
        assertThat(validator.validate(command(validImageBytes("image/jpeg"), "image/jpeg")))
                .isEqualTo("image/jpeg");
    }

    @Test
    void validate_whenContentIsEmpty_rejects() {
        assertThatThrownBy(() -> validator.validate(command(new byte[0], "image/jpeg")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("error.upload.empty");
    }

    @ParameterizedTest
    @ValueSource(strings = {"application/pdf", "text/html", "image/svg+xml", "application/zip"})
    void validate_whenContentTypeIsNotAnAllowedImage_rejects(String contentType) throws Exception {
        assertThatThrownBy(() -> validator.validate(command(validImageBytes("image/jpeg"), contentType)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("error.upload.invalid_type");
    }

    @Test
    void validate_whenContentTypeIsMissing_rejects() throws Exception {
        assertThatThrownBy(() -> validator.validate(command(validImageBytes("image/jpeg"), null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("error.upload.invalid_type");
    }

    @ParameterizedTest
    @ValueSource(strings = {"image/jpeg", "image/png", "image/webp", "image/gif"})
    void validate_acceptsTheSupportedRasterFormats(String contentType) throws Exception {
        assertThat(validator.validate(command(validImageBytes(contentType), contentType)))
                .isEqualTo(contentType);
    }

    @Test
    void validate_ignoresContentTypeParametersAndCasing() throws Exception {
        assertThat(validator.validate(command(validImageBytes("image/jpeg"), "IMAGE/JPEG; charset=binary")))
                .isEqualTo("image/jpeg");
    }

    @Test
    void validate_whenMimeDoesNotMatchBytes_rejects() throws Exception {
        assertThatThrownBy(() -> validator.validate(command(validImageBytes("image/png"), "image/jpeg")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("error.upload.invalid_type");
    }

    @Test
    void validate_whenImageBytesAreMalformed_rejects() {
        assertThatThrownBy(() -> validator.validate(
                command(new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47}, "image/png")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("error.upload.invalid_type");
    }

    @Test
    void validate_whenWebpRiffIsTruncated_rejects() {
        assertThatThrownBy(() -> validator.validate(command(
                new byte[]{'R', 'I', 'F', 'F', 18, 0, 0, 0, 'W', 'E', 'B', 'P'}, "image/webp")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("error.upload.invalid_type");
    }

    @Test
    void validate_whenPngPixelsExceedTheDecodedImageLimit_rejects() throws Exception {
        byte[] imageBytes = validImageBytes("image/png");
        writeInt(imageBytes, 16, 5_000);
        writeInt(imageBytes, 20, 5_000);

        assertThatThrownBy(() -> validator.validate(command(imageBytes, "image/png")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("error.upload.invalid_type");
    }

    @Test
    void validate_whenContentIsLargerThanFiveMiB_rejects() {
        assertThatThrownBy(() -> validator.validate(command(new byte[5 * 1024 * 1024 + 1], "image/jpeg")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("error.upload.too_large");
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
