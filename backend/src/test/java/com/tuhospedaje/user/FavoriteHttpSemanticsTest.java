package com.tuhospedaje.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuhospedaje.AbstractIntegrationTest;
import com.tuhospedaje.configuration.JwtService;
import com.tuhospedaje.entity.Lodging;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.RoleEnum;
import com.tuhospedaje.repository.LodgingRepository;
import com.tuhospedaje.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SC-7.4: DELETE /api/favorites/{id} returns 204 with empty body.
 * SC-7.5: POST /api/favorites/{id} returns 201.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FavoriteHttpSemanticsTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LodgingRepository lodgingRepository;

    @Autowired
    private JwtService jwtService;

    private String userAuthHeader;
    private Long lodgingId;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .firstName("Fav")
                .lastName("Tester")
                .email("fav-http-sem@tuhospedaje.com")
                .password("123456")
                .role(RoleEnum.USER)
                .build();
        User savedUser = userRepository.save(user);
        userAuthHeader = "Bearer " + jwtService.generateToken(savedUser);

        Lodging lodging = new Lodging();
        lodging.setName("Favoritable Hotel");
        lodging.setDescription("Test");
        lodging.setAddress("Calle 1");
        lodging.setCity("Ciudad");
        lodging.setCountry("Pais");
        lodging.setPhoneNumber("111222333");
        lodging.setEmail("fav-hotel@test.com");
        lodging.setPricePerNight(new BigDecimal("100.00"));
        lodging.setMaxGuests(2);
        lodgingId = lodgingRepository.save(lodging).getId();
    }

    // SC-7.5: POST /api/favorites/{lodgingId} returns 201
    @Test
    void addFavorite_returns201() throws Exception {
        Cookie csrfCookie = obtainCsrfCookie(mockMvc);
        mockMvc.perform(post("/api/favorites/{lodgingId}", lodgingId)
                        .header(HttpHeaders.AUTHORIZATION, userAuthHeader)
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue()))
                .andExpect(status().isCreated());
    }

    // SC-7.4: DELETE /api/favorites/{lodgingId} returns 204 with empty body
    @Test
    void removeFavorite_returns204WithNoBody() throws Exception {
        // first add
        Cookie addCsrfCookie = obtainCsrfCookie(mockMvc);
        mockMvc.perform(post("/api/favorites/{lodgingId}", lodgingId)
                        .header(HttpHeaders.AUTHORIZATION, userAuthHeader)
                        .cookie(addCsrfCookie)
                        .header("X-XSRF-TOKEN", addCsrfCookie.getValue()))
                .andExpect(status().isCreated());

        // then remove
        Cookie removeCsrfCookie = obtainCsrfCookie(mockMvc);
        mockMvc.perform(delete("/api/favorites/{lodgingId}", lodgingId)
                        .header(HttpHeaders.AUTHORIZATION, userAuthHeader)
                        .cookie(removeCsrfCookie)
                        .header("X-XSRF-TOKEN", removeCsrfCookie.getValue()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    // GET /api/favorites authenticated — returns list (covers getFavorites controller branch)
    @Test
    void getFavorites_authenticated_returns200WithArray() throws Exception {
        // add one favorite first
        Cookie csrfCookie = obtainCsrfCookie(mockMvc);
        mockMvc.perform(post("/api/favorites/{lodgingId}", lodgingId)
                        .header(HttpHeaders.AUTHORIZATION, userAuthHeader)
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue()))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/favorites")
                        .header(HttpHeaders.AUTHORIZATION, userAuthHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // POST /api/favorites unauthenticated — current behavior: Spring Security returns 403 (no WWW-Authenticate)
    // Pinning current behavior; 401 vs 403 discrepancy noted in apply-progress.
    @Test
    void addFavorite_unauthenticated_returns403() throws Exception {
        // Keep CSRF valid even without auth, so the 403 is attributable to the missing
        // token, not to a missing CSRF header (design's explicit ordering-trap warning).
        Cookie csrfCookie = obtainCsrfCookie(mockMvc);
        mockMvc.perform(post("/api/favorites/{lodgingId}", lodgingId)
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue()))
                .andExpect(status().isForbidden());
    }

    // DELETE /api/favorites unauthenticated — current behavior: 403
    @Test
    void removeFavorite_unauthenticated_returns403() throws Exception {
        Cookie csrfCookie = obtainCsrfCookie(mockMvc);
        mockMvc.perform(delete("/api/favorites/{lodgingId}", lodgingId)
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue()))
                .andExpect(status().isForbidden());
    }

    // GET /api/favorites unauthenticated — current behavior: 403
    @Test
    void getFavorites_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/favorites"))
                .andExpect(status().isForbidden());
    }
}
