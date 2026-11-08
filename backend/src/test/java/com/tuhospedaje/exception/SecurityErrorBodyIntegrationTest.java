package com.tuhospedaje.exception;

import com.tuhospedaje.AbstractIntegrationTest;
import com.tuhospedaje.configuration.JwtService;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.RoleEnum;
import com.tuhospedaje.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 401 and 403 produced by Spring Security must use the same JSON shape as the errors
 * produced by {@link GlobalExceptionHandler}: {@code {"error": "...", "status": N}}.
 *
 * <p>The status codes themselves are part of the existing contract and must not move:
 * some routes answer 401 for an anonymous caller and every other protected route
 * answers 403. Only the body is new.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityErrorBodyIntegrationTest extends AbstractIntegrationTest {

    private static final String AUTH_REQUIRED_ES = "Necesitás iniciar sesión para continuar.";
    private static final String ACCESS_DENIED_ES = "No tenés permisos para realizar esta acción.";
    private static final String AUTH_REQUIRED_EN = "Authentication is required.";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    private Long regularUserId;
    private String regularUserToken;

    @BeforeEach
    void createRegularUser() {
        User saved = userRepository.save(User.builder()
                .firstName("Plain")
                .lastName("User")
                .email("security-error-body-user@tuhospedaje.com")
                .password("123456")
                .role(RoleEnum.USER)
                .build());
        regularUserId = saved.getId();
        regularUserToken = jwtService.generateToken(saved);
    }

    @AfterEach
    void deleteRegularUser() {
        userRepository.deleteById(regularUserId);
    }

    @Test
    void anonymousCallOnA401RouteReturnsJsonBody() throws Exception {
        for (String path : new String[] {"/api/auth/me", "/api/reservations/me"}) {
            mockMvc.perform(get(path).header("Accept-Language", "es"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.error").value(AUTH_REQUIRED_ES));
        }
    }

    @Test
    void anonymousCallOnAnyOtherProtectedRouteReturnsJson403() throws Exception {
        mockMvc.perform(get("/api/users").header("Accept-Language", "es"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value(ACCESS_DENIED_ES));
    }

    @Test
    void authenticatedUserWithoutTheRequiredRoleGetsJson403() throws Exception {
        Cookie csrf = obtainCsrfCookie(mockMvc);

        mockMvc.perform(post("/api/policies")
                        .header("Accept-Language", "es")
                        .cookie(accessCookie(regularUserToken))
                        .cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        // A valid body: request validation runs before @PreAuthorize, so an
                        // invalid one would answer 400 and never reach the role check.
                        .content("{\"name\":\"PL-Sec\",\"description\":\"x\",\"icon\":\"clock\"}"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value(ACCESS_DENIED_ES));
    }

    @Test
    void missingCsrfTokenReturnsJson403() throws Exception {
        mockMvc.perform(post("/api/lodgings")
                        .header("Accept-Language", "es")
                        .cookie(accessCookie(regularUserToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value(ACCESS_DENIED_ES));
    }

    @Test
    void messageFollowsTheAcceptLanguageHeader() throws Exception {
        mockMvc.perform(get("/api/auth/me").header("Accept-Language", "en"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value(AUTH_REQUIRED_EN));
    }
}
