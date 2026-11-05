package com.tuhospedaje.service.impl;

import com.tuhospedaje.dto.upload.UploadResult;
import com.tuhospedaje.exception.UploadException;
import com.tuhospedaje.service.ImageContentValidator;
import com.tuhospedaje.service.ImageStorageService;
import com.tuhospedaje.service.command.UploadImageCommand;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

/**
 * Stores validated images on the local disk under
 * {@code tuhospedaje.uploads.directory}.
 *
 * <p>The stored name is always a server-generated UUID with an extension derived
 * from the validated content type — never from the client-supplied file name or
 * extension (S5: no path traversal, no collisions). Writes are atomic: bytes land
 * in a temporary file inside the hidden {@code .incoming} staging subdirectory
 * (same filesystem, so the move stays atomic) and are then moved onto the final
 * name, so a crash or full disk cannot leave a partial image behind, and no
 * partially-written file is ever visible under {@code /uploads/**}.</p>
 *
 * <p>Returned URLs join {@code tuhospedaje.uploads.public-prefix} with the stored
 * name. The default prefix ({@code /uploads}) is a same-origin path served by the
 * application itself; cross-origin deployments point it at the API origin's public
 * upload URL (absolute, with or without a trailing slash).</p>
 */
@Service
public class LocalImageStorageService implements ImageStorageService {

    private static final Map<String, String> EXTENSION_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "image/gif", "gif");

    private final Path directory;
    private final String publicPrefix;
    private final ImageContentValidator imageContentValidator;

    public LocalImageStorageService(
            @Value("${tuhospedaje.uploads.directory}") String directory,
            @Value("${tuhospedaje.uploads.public-prefix}") String publicPrefix,
            ImageContentValidator imageContentValidator) {
        this.directory = Path.of(directory);
        this.publicPrefix = publicPrefix;
        this.imageContentValidator = imageContentValidator;
    }

    @Override
    public UploadResult store(UploadImageCommand command) {
        String contentType = imageContentValidator.validate(command);
        String extension = EXTENSION_BY_CONTENT_TYPE.get(contentType);
        if (extension == null) {
            // Unreachable while the validator's allow-list and this map stay in sync.
            throw new IllegalStateException("Validated content type has no registered extension: " + contentType);
        }
        String filename = UUID.randomUUID() + "." + extension;
        try {
            // Hidden staging directory for in-flight writes: same filesystem as the
            // target so the final move stays atomic, and never served through
            // /uploads/** (see UploadedImagesWebConfiguration's resource resolver).
            Path incoming = directory.resolve(".incoming");
            Files.createDirectories(incoming);
            Path target = directory.resolve(filename);
            Path temp = Files.createTempFile(incoming, "upload-", ".tmp");
            try {
                Files.write(temp, command.content());
                moveAtomically(temp, target);
            } catch (IOException moveFailure) {
                deleteQuietly(temp);
                throw moveFailure;
            }
            return new UploadResult(buildPublicUrl(filename), filename);
        } catch (IOException e) {
            throw new UploadException("No se pudo guardar la imagen", e);
        }
    }

    /** Joins the configured prefix with the file name, tolerating a trailing slash. */
    private String buildPublicUrl(String filename) {
        String base = publicPrefix.replaceAll("/+$", "");
        return base + "/" + filename;
    }

    private void moveAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void deleteQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // Best-effort cleanup; the store failure is reported to the caller.
        }
    }
}
