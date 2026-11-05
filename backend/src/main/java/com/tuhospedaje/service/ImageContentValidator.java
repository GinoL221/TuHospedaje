package com.tuhospedaje.service;

import com.tuhospedaje.service.command.UploadImageCommand;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;


/**
 * Image content validation, extracted verbatim from the former upload service.
 * Same allowed types, same size and dimension constants, same message keys, same
 * content-type normalization — the storage backend changed, the validation
 * contract did not.
 */
@Component
public class ImageContentValidator {

    /**
     * SVG is deliberately absent: it is XML, it can carry script, and a served
     * SVG would execute from a domain the app links to — a stored-XSS vector
     * that the other raster formats do not have. The frontend's input only
     * advertises {@code accept="image/*"}, which is a hint to the file picker,
     * not a control; this list is the actual gate.
     */
    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;
    private static final int MAX_IMAGE_DIMENSION = 8_192;
    private static final long MAX_IMAGE_PIXELS = 20_000_000L;
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    /**
     * Validates the command and returns the normalized content type of the
     * accepted image, so callers can derive a safe file extension from what was
     * actually validated rather than from anything client-supplied.
     *
     * @throws IllegalArgumentException with a MessageSource key
     *         ({@code error.upload.empty}, {@code error.upload.too_large},
     *         {@code error.upload.invalid_type}) when the image is rejected.
     */
    public String validate(UploadImageCommand command) {
        if (command == null || command.content() == null || command.content().length == 0) {
            throw new IllegalArgumentException("error.upload.empty");
        }

        byte[] imageBytes = command.content();
        if (imageBytes.length > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("error.upload.too_large");
        }

        String contentType = normalizedContentType(command.contentType());
        if (!ALLOWED_CONTENT_TYPES.contains(contentType) || !hasMatchingValidImageBytes(contentType, imageBytes)) {
            throw new IllegalArgumentException("error.upload.invalid_type");
        }
        return contentType;
    }

    private boolean hasMatchingValidImageBytes(String contentType, byte[] imageBytes) {
        return switch (contentType) {
            case "image/jpeg" -> hasPrefix(imageBytes, (byte) 0xFF, (byte) 0xD8, (byte) 0xFF)
                    && decodesWithImageIo(imageBytes);
            case "image/png" -> hasPrefix(imageBytes, PNG_SIGNATURE) && decodesWithImageIo(imageBytes);
            case "image/gif" -> (hasPrefix(imageBytes, "GIF87a".getBytes())
                    || hasPrefix(imageBytes, "GIF89a".getBytes())) && decodesWithImageIo(imageBytes);
            case "image/webp" -> isStructurallyValidWebp(imageBytes)
                    && webpDecodesWhenAvailable(imageBytes);
            default -> false;
        };
    }

    private boolean decodesWithImageIo(byte[] imageBytes) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(imageBytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                return false;
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input);
                return isWithinDecodedLimits(reader.getWidth(0), reader.getHeight(0))
                        && ImageIO.read(new ByteArrayInputStream(imageBytes)) != null;
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            return false;
        }
    }

    private boolean isWithinDecodedLimits(int width, int height) {
        return width > 0 && height > 0 && width <= MAX_IMAGE_DIMENSION && height <= MAX_IMAGE_DIMENSION
                && (long) width * height <= MAX_IMAGE_PIXELS;
    }

    private boolean webpDecodesWhenAvailable(byte[] imageBytes) {
        if (!ImageIO.getImageReadersByMIMEType("image/webp").hasNext()) {
            return true;
        }
        return decodesWithImageIo(imageBytes);
    }

    /** Validates the complete RIFF layout, VP8-family header, and decoded-size limits. */
    private boolean isStructurallyValidWebp(byte[] bytes) {
        if (bytes.length < 26 || !hasPrefix(bytes, "RIFF".getBytes())
                || !hasPrefixAt(bytes, 8, "WEBP".getBytes())
                || littleEndianUnsignedInt(bytes, 4) != bytes.length - 8L) {
            return false;
        }
        int offset = 12;
        boolean imageDataChunkFound = false;
        while (offset < bytes.length) {
            if (bytes.length - offset < 8) {
                return false;
            }
            long chunkLength = littleEndianUnsignedInt(bytes, offset + 4);
            long chunkEnd = offset + 8L + chunkLength;
            if (chunkEnd > bytes.length || chunkEnd + (chunkLength & 1) > bytes.length) {
                return false;
            }
            if (hasPrefixAt(bytes, offset, "VP8 ".getBytes())) {
                if (!validVp8Dimensions(bytes, offset, chunkLength)) {
                    return false;
                }
                imageDataChunkFound = true;
            } else if (hasPrefixAt(bytes, offset, "VP8L".getBytes())) {
                if (!validVp8lDimensions(bytes, offset, chunkLength)) {
                    return false;
                }
                imageDataChunkFound = true;
            } else if (hasPrefixAt(bytes, offset, "VP8X".getBytes())
                    && !validVp8xDimensions(bytes, offset, chunkLength)) {
                return false;
            }
            offset = (int) (chunkEnd + (chunkLength & 1));
        }
        return imageDataChunkFound && offset == bytes.length;
    }

    private boolean validVp8Dimensions(byte[] bytes, int offset, long length) {
        int data = offset + 8;
        if (length < 10 || bytes[data + 3] != (byte) 0x9D || bytes[data + 4] != 0x01 || bytes[data + 5] != 0x2A) {
            return false;
        }
        int width = (bytes[data + 6] & 0xFF) | ((bytes[data + 7] & 0x3F) << 8);
        int height = (bytes[data + 8] & 0xFF) | ((bytes[data + 9] & 0x3F) << 8);
        return isWithinDecodedLimits(width, height);
    }

    private boolean validVp8lDimensions(byte[] bytes, int offset, long length) {
        int data = offset + 8;
        if (length < 5 || bytes[data] != 0x2F) {
            return false;
        }
        int width = 1 + (bytes[data + 1] & 0xFF) + ((bytes[data + 2] & 0x3F) << 8);
        int height = 1 + ((bytes[data + 2] & 0xC0) >>> 6) + ((bytes[data + 3] & 0xFF) << 2)
                + ((bytes[data + 4] & 0x0F) << 10);
        return isWithinDecodedLimits(width, height);
    }

    private boolean validVp8xDimensions(byte[] bytes, int offset, long length) {
        if (length != 10) {
            return false;
        }
        int data = offset + 8;
        int width = 1 + (bytes[data + 4] & 0xFF) + ((bytes[data + 5] & 0xFF) << 8) + ((bytes[data + 6] & 0xFF) << 16);
        int height = 1 + (bytes[data + 7] & 0xFF) + ((bytes[data + 8] & 0xFF) << 8) + ((bytes[data + 9] & 0xFF) << 16);
        return isWithinDecodedLimits(width, height);
    }

    private long littleEndianUnsignedInt(byte[] bytes, int offset) {
        return ((long) bytes[offset] & 0xFF)
                | (((long) bytes[offset + 1] & 0xFF) << 8)
                | (((long) bytes[offset + 2] & 0xFF) << 16)
                | (((long) bytes[offset + 3] & 0xFF) << 24);
    }

    private boolean hasPrefix(byte[] bytes, byte... prefix) {
        return hasPrefixAt(bytes, 0, prefix);
    }

    private boolean hasPrefixAt(byte[] bytes, int offset, byte... prefix) {
        return bytes.length >= offset + prefix.length
                && Arrays.equals(Arrays.copyOfRange(bytes, offset, offset + prefix.length), prefix);
    }

    /**
     * Content-Type arrives with parameters and arbitrary casing in the wild
     * ({@code IMAGE/JPEG; charset=binary}); only the media type decides. A missing header
     * normalizes to the empty string, which is not in the allow-list, so it is refused.
     */
    private String normalizedContentType(String contentType) {
        if (contentType == null) {
            return "";
        }
        int parameterSeparator = contentType.indexOf(';');
        String mediaType = parameterSeparator < 0 ? contentType : contentType.substring(0, parameterSeparator);
        return mediaType.trim().toLowerCase(Locale.ROOT);
    }
}
