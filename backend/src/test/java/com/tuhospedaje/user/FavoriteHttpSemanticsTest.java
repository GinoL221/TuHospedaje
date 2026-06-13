package com.tuhospedaje.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuhospedaje.AbstractIntegrationTest;
import com.tuhospedaje.configuration.JwtService;
import com.tuhospedaje.entity.Lodging;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.RoleEnum;
import com.tuhospedaje.repository.LodgingRepository;
import com.tuhospedaje.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
        mockMvc.perform(post("/api/favorites/{lodgingId}", lodgingId)
                        .header(HttpHeaders.AUTHORIZATION, userAuthHeader))
                .andExpect(status().isCreated());
    }

    // SC-7.4: DELETE /api/favorites/{lodgingId} returns 204 with empty body
    @Test
    void removeFavorite_returns204WithNoBody() throws Exception {
        // first add
        mockMvc.perform(post("/api/favorites/{lodgingId}", lodgingId)
                        .header(HttpHeaders.AUTHORIZATION, userAuthHeader))
                .andExpect(status().isCreated());

        // then remove
        mockMvc.perform(delete("/api/favorites/{lodgingId}", lodgingId)
                        .header(HttpHeaders.AUTHORIZATION, userAuthHeader))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }
}
