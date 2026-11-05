package com.tuhospedaje.upload;

import com.tuhospedaje.AbstractIntegrationTest;
import com.tuhospedaje.configuration.JwtService;
import com.tuhospedaje.configuration.TestcontainersConfiguration;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.RoleEnum;
import com.tuhospedaje.repository.EmailOutboxRepository;
import com.tuhospedaje.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP contract for local image uploads (S2–S7): POST /api/upload stays ADMIN-only and
 * CSRF-protected, a stored image is served publicly from /uploads/**, and the resource
 * handler is confined to the uploads directory (no ".." traversal).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class LocalImageStorageIntegrationTest extends AbstractIntegrationTest {

    private static final Path UPLOADS_DIR = createUploadsDir();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailOutboxRepository emailOutboxRepository;

    @Autowired
    private JwtService jwtService;

    private String adminToken;

    @DynamicPropertySource
    static void uploadsProperties(DynamicPropertyRegistry registry) {
        registry.add("tuhospedaje.uploads.directory", UPLOADS_DIR::toString);
    }

    @BeforeEach
    void setUp() {
        // Outbox rows reference users through FK_email_outbox_user, so they go first.
        // Without this, the suite fails only when another test ran before this one.
        emailOutboxRepository.deleteAll();
        userRepository.deleteAll();
        User admin = userRepository.save(User.builder()
                .firstName("Admin")
                .lastName("Storage")
                .email("admin-storage@test.com")
                .password("hash")
                .role(RoleEnum.ADMIN)
                .build());
        adminToken = jwtService.generateToken(admin);
    }

    @Test
    void postWithoutAdminSession_returns403() throws Exception {
        jakarta.servlet.http.Cookie csrfCookie = obtainCsrfCookie(mockMvc);

        mockMvc.perform(multipart("/api/upload")
                        .file(validJpegPart())
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue()))
                .andExpect(status().isForbidden());
    }

    @Test
    void postWithValidFileAsAdmin_returns200WithUploadsUrl() throws Exception {
        jakarta.servlet.http.Cookie csrfCookie = obtainCsrfCookie(mockMvc);

        MvcResult result = mockMvc.perform(multipart("/api/upload")
                        .file(validJpegPart())
                        .cookie(accessCookie(adminToken))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").isNotEmpty())
                .andExpect(jsonPath("$.publicId").isNotEmpty())
                .andReturn();

        String url = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.url");
        assertThat(url).startsWith("/uploads/");
    }

    @Test
    void getStoredUpload_isPubliclyAccessibleWithoutAuthentication() throws Exception {
        jakarta.servlet.http.Cookie csrfCookie = obtainCsrfCookie(mockMvc);

        MvcResult result = mockMvc.perform(multipart("/api/upload")
                        .file(validJpegPart())
                        .cookie(accessCookie(adminToken))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue()))
                .andExpect(status().isOk())
                .andReturn();
        String url = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.url");

        mockMvc.perform(get(url))
                .andExpect(status().isOk());
    }

    /** S7: HTTP semantics — HEAD must be as public as GET for <img> clients and caches. */
    @Test
    void headStoredUpload_isPubliclyAccessibleWithoutAuthentication() throws Exception {
        String url = uploadAsAdmin();

        mockMvc.perform(head(url))
                .andExpect(status().isOk());
    }

    /** A missing image is the caller's input: 404 without noise, regardless of authentication. */
    @Test
    void missingUpload_returns404WithoutAuthentication() throws Exception {
        mockMvc.perform(get("/uploads/does-not-exist.jpg"))
                .andExpect(status().isNotFound());
    }

    /**
     * Write-in-progress temp files live under the hidden .incoming directory inside the
     * uploads root; that directory must never be reachable through /uploads/**.
     */
    @Test
    void hiddenIncomingDirectory_isNeverServed() throws Exception {
        Path incoming = UPLOADS_DIR.resolve(".incoming");
        Files.createDirectories(incoming);
        Files.writeString(incoming.resolve("leftover.tmp"), "half-written-bytes");

        mockMvc.perform(get("/uploads/.incoming/leftover.tmp"))
                .andExpect(status().isNotFound());

        // A stored, visible file keeps being served while .incoming is blocked.
        String url = uploadAsAdmin();
        mockMvc.perform(get(url))
                .andExpect(status().isOk());
    }

    /** S5: the served path must stay confined to the uploads directory. */
    @Test
    void getUploadsWithTraversalSegments_cannotEscapeTheUploadsDirectory() throws Exception {
        String secret = "top-secret-non-image-marker";
        // The decoy lives in its own temp dir, not next to the uploads root, and is
        // always removed: the test must not leave artifacts behind in /tmp.
        Path decoyDir = Files.createTempDirectory("tuhospedaje-traversal-decoy-");
        try {
            Path secretFile = decoyDir.resolve("secret.txt");
            Files.writeString(secretFile, secret);
            String secretUrl = "/uploads/../" + decoyDir.getFileName() + "/secret.txt";

            mockMvc.perform(get(secretUrl))
                    .andExpect(status().is4xxClientError())
                    .andExpect(mvcResult ->
                            assertThat(mvcResult.getResponse().getContentAsString()).doesNotContain(secret));

            mockMvc.perform(get("/uploads/%2e%2e/" + decoyDir.getFileName() + "/secret.txt"))
                    .andExpect(status().is4xxClientError())
                    .andExpect(mvcResult ->
                            assertThat(mvcResult.getResponse().getContentAsString()).doesNotContain(secret));
        } finally {
            Files.deleteIfExists(decoyDir.resolve("secret.txt"));
            Files.deleteIfExists(decoyDir);
        }
    }

    private String uploadAsAdmin() throws Exception {
        jakarta.servlet.http.Cookie csrfCookie = obtainCsrfCookie(mockMvc);
        MvcResult result = mockMvc.perform(multipart("/api/upload")
                        .file(validJpegPart())
                        .cookie(accessCookie(adminToken))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue()))
                .andExpect(status().isOk())
                .andReturn();
        return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.url");
    }

    private static Path createUploadsDir() {
        try {
            return Files.createTempDirectory("tuhospedaje-uploads-");
        } catch (java.io.IOException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    private MockMultipartFile validJpegPart() throws Exception {
        return new MockMultipartFile("file", "client-name.jpg", "image/jpeg", validJpegBytes());
    }

    private byte[] validJpegBytes() throws Exception {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "jpg", output)) {
            throw new IllegalStateException("No ImageIO writer for jpg");
        }
        return output.toByteArray();
    }
}
