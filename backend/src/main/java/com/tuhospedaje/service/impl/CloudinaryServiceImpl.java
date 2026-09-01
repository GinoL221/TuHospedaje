package com.tuhospedaje.service.impl;

import com.cloudinary.Cloudinary;
import com.tuhospedaje.dto.upload.UploadResult;
import com.tuhospedaje.exception.UploadException;
import com.tuhospedaje.service.CloudinaryService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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

    private final Cloudinary cloudinary;

    public CloudinaryServiceImpl(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public UploadResult uploadImage(MultipartFile file) {
        validate(file);
        try {
            Map raw = cloudinary.uploader().upload(file.getBytes(), Map.of());
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
     * Size is not checked here: {@code spring.servlet.multipart.max-file-size} stops an
     * oversized part during parsing, before this method ever runs, and that path answers
     * 413 via {@code handleMaxUploadSizeExceeded}.
     */
    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("error.upload.empty");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(normalizedContentType(file))) {
            throw new IllegalArgumentException("error.upload.invalid_type");
        }
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
