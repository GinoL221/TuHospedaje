package com.tuhospedaje.lodging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuhospedaje.AbstractIntegrationTest;
import com.tuhospedaje.configuration.JwtService;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SC-7.1: PUT /api/lodgings/{id} with invalid body returns 400 + validation error shape.
 * SC-7.3: DELETE /api/lodgings/{id} returns 204 with empty body.
 */
@SpringBootTest
@AutoConfigureMockMvc
class LodgingHttpSemanticsTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LodgingRepository lodgingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    private String adminAuthHeader;

    @BeforeEach
    void setUp() {
        User admin = User.builder()
                .firstName("Admin")
                .lastName("Http")
                .email("admin-http-semantics@tuhospedaje.com")
                .password("123456")
                .role(RoleEnum.ADMIN)
                .build();
        User savedAdmin = userRepository.save(admin);
        adminAuthHeader = jwtService.generateToken(savedAdmin);
    }

    // SC-7.1: PUT with invalid body (blank name) should return 400
    @Test
    void updateLodging_withBlankName_returns400() throws Exception {
        Long id = createTestLodging();

        Map<String, Object> invalidBody = Map.of(
                "name", "",
                "address", "Calle 123",
                "city", "Ciudad",
                "country", "País",
                "phoneNumber", "123456789",
                "email", "put-invalid@test.com"
        );

        Cookie csrfCookie = obtainCsrfCookie(mockMvc);
        mockMvc.perform(put("/api/lodgings/{id}", id)
                        .cookie(accessCookie(adminAuthHeader))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // SC-7.3: DELETE /api/lodgings/{id} returns 204 with no body
    @Test
    void deleteLodging_returns204WithNoBody() throws Exception {
        Long id = createTestLodging();

        Cookie csrfCookie = obtainCsrfCookie(mockMvc);
        mockMvc.perform(delete("/api/lodgings/{id}", id)
                        .cookie(accessCookie(adminAuthHeader))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    private Long createTestLodging() throws Exception {
        Map<String, Object> request = Map.of(
                "name", "HTTP Semantics Hotel",
                "description", "Test",
                "address", "Calle 100",
                "city", "TestCity",
                "country", "TestCountry",
                "phoneNumber", "555000111",
                "email", "http-sem@test.com",
                "pricePerNight", new BigDecimal("30000.00"),
                "maxGuests", 4
        );

        Cookie csrfCookie = obtainCsrfCookie(mockMvc);
        String response = mockMvc.perform(post("/api/lodgings")
                        .cookie(accessCookie(adminAuthHeader))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }
}
