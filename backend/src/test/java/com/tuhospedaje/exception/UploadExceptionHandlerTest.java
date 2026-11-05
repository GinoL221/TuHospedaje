package com.tuhospedaje.exception;

import com.tuhospedaje.AbstractIntegrationTest;
import com.tuhospedaje.configuration.JwtService;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.RoleEnum;
import com.tuhospedaje.repository.EmailOutboxRepository;
import com.tuhospedaje.repository.LodgingRepository;
import com.tuhospedaje.repository.RatingRepository;
import com.tuhospedaje.repository.ReservationRepository;
import com.tuhospedaje.repository.UserRepository;
import com.tuhospedaje.service.ImageStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UploadException → standard JSON shape with no storage internals leaked. Storage is
 * the server's own disk, so a failed write is a 500 — there is no upstream provider
 * whose failure would map to 502 anymore.
 */
@SpringBootTest
@AutoConfigureMockMvc
class UploadExceptionHandlerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailOutboxRepository emailOutboxRepository;

    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private LodgingRepository lodgingRepository;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private ImageStorageService imageStorageService;

    private String adminToken;

    @BeforeEach
    void setUp() {
        // Outbox rows reference users through FK_email_outbox_user, so they go first.
        // Without this, the suite fails only when another test ran before this one.
        emailOutboxRepository.deleteAll();
        ratingRepository.deleteAll();
        reservationRepository.deleteAll();
        lodgingRepository.deleteAll();
        userRepository.deleteAll();
        User admin = userRepository.save(User.builder()
                .firstName("Admin")
                .lastName("Upload")
                .email("admin-upload@test.com")
                .password("hash")
                .role(RoleEnum.ADMIN)
                .build());
        adminToken = jwtService.generateToken(admin);
    }

    /** A failed write to the uploads directory returns 500 with the generic message, no storage internals leaked */
    @Test
    void storageWriteFailure_returns500StandardShape() throws Exception {
        when(imageStorageService.store(any()))
                .thenThrow(new UploadException("No se pudo guardar la imagen",
                        new RuntimeException("Permission denied: /var/lib/uploads - internal detail")));

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "fake-image-bytes".getBytes());

        jakarta.servlet.http.Cookie csrfCookie = obtainCsrfCookie(mockMvc);
        mockMvc.perform(multipart("/api/upload")
                        .file(file)
                        .cookie(accessCookie(adminToken))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").isString())
                .andExpect(jsonPath("$.error").value(
                        org.hamcrest.Matchers.not(
                                org.hamcrest.Matchers.containsString("Permission denied"))));
    }

    /**
     * A rejected file is the caller's input, so it must be 400 — not the 500 that a
     * failed write would produce. Unlike the codebase's older
     * IllegalArgumentException throw sites (hardcoded literals that fall back verbatim),
     * the upload guards throw MessageSource keys, so this also proves the key branch of
     * handleIllegalArgument actually resolves instead of echoing "error.upload.invalid_type".
     */
    @Test
    void rejectedFileType_returns400WithResolvedMessage() throws Exception {
        when(imageStorageService.store(any()))
                .thenThrow(new IllegalArgumentException("error.upload.invalid_type"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "payload.pdf", "application/pdf", "not-an-image".getBytes());

        jakarta.servlet.http.Cookie csrfCookie = obtainCsrfCookie(mockMvc);
        mockMvc.perform(multipart("/api/upload")
                        .file(file)
                        .cookie(accessCookie(adminToken))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Only JPEG, PNG, WebP and GIF images are allowed."));
    }

    /** The direct service size guard retains the localized 400 response contract. */
    @Test
    void serviceLevelSizeRejection_returns400WithResolvedMessage() throws Exception {
        when(imageStorageService.store(any()))
                .thenThrow(new IllegalArgumentException("error.upload.too_large"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "large.jpg", "image/jpeg", "valid-parser-size-bytes".getBytes());

        jakarta.servlet.http.Cookie csrfCookie = obtainCsrfCookie(mockMvc);
        mockMvc.perform(multipart("/api/upload")
                        .file(file)
                        .cookie(accessCookie(adminToken))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("The image exceeds the maximum allowed size."));
    }

    /** Same guard, empty part: still the caller's input, still 400. */
    @Test
    void emptyFile_returns400WithResolvedMessage() throws Exception {
        when(imageStorageService.store(any()))
                .thenThrow(new IllegalArgumentException("error.upload.empty"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.jpg", "image/jpeg", new byte[0]);

        jakarta.servlet.http.Cookie csrfCookie = obtainCsrfCookie(mockMvc);
        mockMvc.perform(multipart("/api/upload")
                        .file(file)
                        .cookie(accessCookie(adminToken))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("The uploaded file is empty."));
    }
}
