package com.tuhospedaje.exception;

import com.tuhospedaje.AbstractIntegrationTest;
import com.tuhospedaje.configuration.JwtService;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.RoleEnum;
import com.tuhospedaje.repository.LodgingRepository;
import com.tuhospedaje.repository.RatingRepository;
import com.tuhospedaje.repository.ReservationRepository;
import com.tuhospedaje.repository.UserRepository;
import com.tuhospedaje.service.CloudinaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
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
 * SC-6.2: UploadException → 502 standard JSON shape, no Cloudinary internals leaked.
 */
@SpringBootTest
@AutoConfigureMockMvc
class UploadExceptionHandlerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private LodgingRepository lodgingRepository;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private CloudinaryService cloudinaryService;

    private String adminToken;

    @BeforeEach
    void setUp() {
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

    /** SC-6.2: Cloudinary upload failure returns 502 with standard shape, no internal message leaked */
    @Test
    void cloudinaryUploadFailure_returns502StandardShape() throws Exception {
        when(cloudinaryService.uploadImage(any()))
                .thenThrow(new UploadException("No se pudo subir la imagen",
                        new RuntimeException("Cloudinary API key invalid - internal detail")));

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "fake-image-bytes".getBytes());

        jakarta.servlet.http.Cookie csrfCookie = obtainCsrfCookie(mockMvc);
        mockMvc.perform(multipart("/api/upload")
                        .file(file)
                        .cookie(accessCookie(adminToken))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue()))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.error").isString())
                .andExpect(jsonPath("$.error").value(
                        org.hamcrest.Matchers.not(
                                org.hamcrest.Matchers.containsString("Cloudinary API key invalid"))));
    }

    /**
     * A rejected file is the caller's input, so it must be 400 — not the 502 that a
     * Cloudinary round-trip would produce. Unlike the codebase's older
     * IllegalArgumentException throw sites (hardcoded literals that fall back verbatim),
     * the upload guards throw MessageSource keys, so this also proves the key branch of
     * handleIllegalArgument actually resolves instead of echoing "error.upload.invalid_type".
     */
    @Test
    void rejectedFileType_returns400WithResolvedMessage() throws Exception {
        when(cloudinaryService.uploadImage(any()))
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

    /** Same guard, empty part: still the caller's input, still 400. */
    @Test
    void emptyFile_returns400WithResolvedMessage() throws Exception {
        when(cloudinaryService.uploadImage(any()))
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
