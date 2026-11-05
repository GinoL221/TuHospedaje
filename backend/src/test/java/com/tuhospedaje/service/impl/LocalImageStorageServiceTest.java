package com.tuhospedaje.service.impl;

import com.tuhospedaje.dto.upload.UploadResult;
import com.tuhospedaje.exception.UploadException;
import com.tuhospedaje.service.ImageContentValidator;
import com.tuhospedaje.service.command.UploadImageCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalImageStorageServiceTest {

    /** Stored names are server-generated UUIDs; the client name never reaches the disk. */
    private static final Pattern STORED_NAME_PATTERN =
            Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|webp|gif)$");

    @TempDir
    Path tempDir;

    private LocalImageStorageService serviceIn(Path directory) {
        return serviceIn(directory, "/uploads");
    }

    private LocalImageStorageService serviceIn(Path directory, String publicPrefix) {
        return new LocalImageStorageService(directory.toString(), publicPrefix, new ImageContentValidator());
    }

    @Test
    void store_savesTheFileAndReturnsAnUploadsUrl() throws Exception {
        LocalImageStorageService service = serviceIn(tempDir);
        byte[] imageBytes = validImageBytes("image/jpeg");

        UploadResult result = service.store(command(imageBytes, "image/jpeg"));

        assertThat(result.getUrl()).startsWith("/uploads/");
        String storedName = result.getPublicId();
        assertThat(result.getUrl()).isEqualTo("/uploads/" + storedName);
        assertThat(storedName).matches(STORED_NAME_PATTERN);
        assertThat(Files.readAllBytes(tempDir.resolve(storedName))).isEqualTo(imageBytes);
    }

    /** Default prefix: same-origin relative path, resolved against the API origin. */
    @Test
    void store_withTheDefaultPrefix_buildsARelativeUploadsUrl() throws Exception {
        UploadResult result = serviceIn(tempDir, "/uploads")
                .store(command(validImageBytes("image/png"), "image/png"));

        assertThat(result.getUrl()).isEqualTo("/uploads/" + result.getPublicId());
        assertThat(result.getPublicId()).matches(STORED_NAME_PATTERN);
    }

    /** Cross-origin setups point the prefix at the API origin; the slash must not double. */
    @Test
    void store_withAnAbsolutePrefixEndingInASlash_doesNotDoubleTheSlash() throws Exception {
        UploadResult result = serviceIn(tempDir, "http://127.0.0.1:8080/uploads/")
                .store(command(validImageBytes("image/png"), "image/png"));

        assertThat(result.getUrl()).isEqualTo("http://127.0.0.1:8080/uploads/" + result.getPublicId());
    }

    @Test
    void store_withAnAbsolutePrefixWithoutTrailingSlash_addsTheSeparator() throws Exception {
        UploadResult result = serviceIn(tempDir, "http://127.0.0.1:8080/uploads")
                .store(command(validImageBytes("image/png"), "image/png"));

        assertThat(result.getUrl()).isEqualTo("http://127.0.0.1:8080/uploads/" + result.getPublicId());
    }

    @Test
    void store_twoUploadsProduceDistinctNames() throws Exception {
        LocalImageStorageService service = serviceIn(tempDir);

        UploadResult first = service.store(command(validImageBytes("image/png"), "image/png"));
        UploadResult second = service.store(command(validImageBytes("image/png"), "image/png"));

        assertThat(first.getPublicId()).isNotEqualTo(second.getPublicId());
        // Visible files only: the hidden .incoming staging directory must not leak
        // into the served uploads root (covered end-to-end in the integration test).
        assertThat(list(tempDir)).containsExactlyInAnyOrder(
                tempDir.resolve(first.getPublicId()), tempDir.resolve(second.getPublicId()),
                tempDir.resolve(".incoming"));
        assertThat(tempDir.resolve(".incoming")).isDirectory();
    }

    @ParameterizedTest
    @ValueSource(strings = {"image/jpeg", "image/png", "image/webp", "image/gif"})
    void store_derivesTheExtensionFromTheValidatedContentType(String contentType) throws Exception {
        UploadResult result = serviceIn(tempDir).store(command(validImageBytes(contentType), contentType));

        String expectedExtension = contentType.substring("image/".length()).replace("jpeg", "jpg");
        assertThat(result.getPublicId()).endsWith("." + expectedExtension);
    }

    @Test
    void store_theStoredNameNeverDerivesFromTheClientName() throws Exception {
        // UploadImageCommand carries no client filename at all, and the extension comes
        // from the validated content type — the client-controlled bytes are content only.
        Set<String> seenNames = new HashSet<>();
        LocalImageStorageService service = serviceIn(tempDir);
        for (int i = 0; i < 5; i++) {
            UploadResult result = service.store(command(validImageBytes("image/gif"), "image/gif"));
            assertThat(result.getPublicId()).matches(STORED_NAME_PATTERN);
            seenNames.add(result.getPublicId());
        }
        assertThat(seenNames).hasSize(5);
    }

    @Test
    void store_rejectsEmptyContentBeforeWriting() {
        assertThatThrownBy(() -> serviceIn(tempDir).store(command(new byte[0], "image/jpeg")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("error.upload.empty");
        assertThat(list(tempDir)).isEmpty();
    }

    @Test
    void store_rejectsOversizedContentBeforeWriting() {
        assertThatThrownBy(() -> serviceIn(tempDir).store(command(new byte[5 * 1024 * 1024 + 1], "image/jpeg")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("error.upload.too_large");
        assertThat(list(tempDir)).isEmpty();
    }

    @Test
    void store_rejectsMismatchedMagicBytesBeforeWriting() {
        byte[] notAnImage = "definitely not a JPEG".getBytes();
        assertThatThrownBy(() -> serviceIn(tempDir).store(command(notAnImage, "image/jpeg")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("error.upload.invalid_type");
        assertThat(list(tempDir)).isEmpty();
    }

    @Test
    void store_createsTheDirectoryWhenItDoesNotExist() throws Exception {
        Path missing = tempDir.resolve("nested/uploads");

        UploadResult result = serviceIn(missing).store(command(validImageBytes("image/png"), "image/png"));

        assertThat(missing).isDirectory();
        assertThat(missing.resolve(result.getPublicId())).exists();
    }

    @Test
    void store_whenWritingFails_throwsUploadException() throws Exception {
        // A regular file where the directory should be makes every write fail.
        Path notADirectory = tempDir.resolve("blocker");
        Files.writeString(notADirectory, "in the way");

        assertThatThrownBy(() -> serviceIn(notADirectory)
                .store(command(validImageBytes("image/jpeg"), "image/jpeg")))
                .isInstanceOf(UploadException.class)
                .hasMessage("No se pudo guardar la imagen");
    }

    private UploadImageCommand command(byte[] content, String contentType) {
        return new UploadImageCommand(content, contentType);
    }

    private java.util.List<Path> list(Path dir) {
        try (java.util.stream.Stream<Path> entries = Files.list(dir)) {
            return entries.toList();
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    private byte[] validImageBytes(String contentType) throws IOException {
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
