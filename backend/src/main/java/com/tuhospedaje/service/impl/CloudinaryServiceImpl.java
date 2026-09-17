package com.tuhospedaje.service.impl;

import com.cloudinary.Cloudinary;
import com.tuhospedaje.dto.upload.UploadResult;
import com.tuhospedaje.exception.UploadException;
import com.tuhospedaje.service.CloudinaryService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class CloudinaryServiceImpl implements CloudinaryService {

    /**
     * SVG is deliberately absent: it is XML, it can carry script, and Cloudinary serves
     * it back from a domain the app links to — a stored-XSS vector that the other raster
     * formats do not have. The frontend's input only advertises {@code accept="image/*"},
     * which is a hint to the file picker, not a control; this list is the actual gate.
     */
    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;
    private static final int MAX_IMAGE_DIMENSION = 8_192;
    private static final long MAX_IMAGE_PIXELS = 20_000_000L;
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    private final Cloudinary cloudinary;

    public CloudinaryServiceImpl(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public UploadResult uploadImage(MultipartFile file) {
        byte[] imageBytes = validate(file);
        try {
            Map raw = cloudinary.uploader().upload(imageBytes, Map.of());
            return new UploadResult(raw);
        } catch (Exception e) {
            throw new UploadException("No se pudo subir la imagen", e);
        }
    }

    /**
     * Rejects before the network call so a bad part comes back as a 400 the caller can
     * fix, not the 502 a Cloudinary round-trip would produce (which reads as "the image
     * provider is down"). Messages are MessageSource keys — {@code
     * GlobalExceptionHandler.handleIllegalArgument} resolves them per locale.
     * <p>
     * The parser still rejects oversized requests as 413 before controller invocation.
     * This second 5 MiB guard protects direct/service invocations and returns the existing
     * localized 400 contract before allocating or sending bytes to Cloudinary.
     */
    private byte[] validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("error.upload.empty");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("error.upload.too_large");
        }

        String contentType = normalizedContentType(file);
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("error.upload.invalid_type");
        }

        try {
            byte[] imageBytes = file.getBytes();
            if (imageBytes.length > MAX_FILE_SIZE_BYTES) {
                throw new IllegalArgumentException("error.upload.too_large");
            }
            if (!hasMatchingValidImageBytes(contentType, imageBytes)) {
                throw new IllegalArgumentException("error.upload.invalid_type");
            }
            return imageBytes;
        } catch (IOException e) {
            throw new UploadException("No se pudo leer la imagen", e);
        }
    }

    private boolean hasMatchingValidImageBytes(String contentType, byte[] imageBytes) {
        return switch (contentType) {
            case "image/jpeg" -> hasPrefix(imageBytes, (byte) 0xFF, (byte) 0xD8, (byte) 0xFF)
                    && decodesWithImageIo(imageBytes);
            case "image/png" -> hasPrefix(imageBytes, PNG_SIGNATURE) && decodesWithImageIo(imageBytes);
            case "image/gif" -> (hasPrefix(imageBytes, "GIF87a".getBytes())
                    || hasPrefix(imageBytes, "GIF89a".getBytes())) && decodesWithImageIo(imageBytes);
            case "image/webp" -> isStructurallyValidWebp(imageBytes) && webpDecodesWhenAvailable(imageBytes);
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
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width > MAX_IMAGE_DIMENSION || height > MAX_IMAGE_DIMENSION
                        || (long) width * height > MAX_IMAGE_PIXELS) {
                    return false;
                }
            } finally {
                reader.dispose();
            }
            return ImageIO.read(new ByteArrayInputStream(imageBytes)) != null;
        } catch (IOException e) {
            return false;
        }
    }

    private boolean webpDecodesWhenAvailable(byte[] imageBytes) {
        if (!ImageIO.getImageReadersByMIMEType("image/webp").hasNext()) {
            // The stock JDK has no WebP reader. The strict RIFF container and VP8-family
            // chunk checks above are the available runtime validation in that deployment.
            return true;
        }
        return decodesWithImageIo(imageBytes);
    }

    /** Validates the RIFF length plus WEBP and VP8/VP8L/VP8X chunk signatures. */
    private boolean isStructurallyValidWebp(byte[] bytes) {
        if (bytes.length < 20 || !hasPrefix(bytes, "RIFF".getBytes())
                || !hasPrefixAt(bytes, 8, "WEBP".getBytes())) {
            return false;
        }
        long riffLength = littleEndianUnsignedInt(bytes, 4);
        if (riffLength != bytes.length - 8L) {
            return false;
        }
        boolean supportedChunk = hasPrefixAt(bytes, 12, "VP8 ".getBytes())
                || hasPrefixAt(bytes, 12, "VP8L".getBytes())
                || hasPrefixAt(bytes, 12, "VP8X".getBytes());
        long firstChunkLength = littleEndianUnsignedInt(bytes, 16);
        return supportedChunk && firstChunkLength <= bytes.length - 20L;
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
     * normalizes to the empty string, which is not in the allow-list, so it is refused
     * rather than guessed.
     */
    private String normalizedContentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null) {
            return "";
        }
        int parameterSeparator = contentType.indexOf(';');
        String mediaType = parameterSeparator < 0 ? contentType : contentType.substring(0, parameterSeparator);
        return mediaType.trim().toLowerCase(Locale.ROOT);
    }
}
