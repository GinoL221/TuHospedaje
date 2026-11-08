package com.tuhospedaje.configuration;

import com.tuhospedaje.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiSecurityDocumentationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("OpenAPI reflects 403 (and no 401) on endpoints protected by default entry point")
    void openApiReflectsForbiddenOnDefaultProtectedEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                // /api/users (GET)
                .andExpect(jsonPath("$.paths['/api/users'].get.responses", hasKey("403")))
                .andExpect(jsonPath("$.paths['/api/users'].get.responses", not(hasKey("401"))))
                // /api/users/{id}/role (PUT)
                .andExpect(jsonPath("$.paths['/api/users/{id}/role'].put.responses", hasKey("403")))
                .andExpect(jsonPath("$.paths['/api/users/{id}/role'].put.responses", not(hasKey("401"))))
                // /api/users/{id}/enabled (PATCH)
                .andExpect(jsonPath("$.paths['/api/users/{id}/enabled'].patch.responses", hasKey("403")))
                .andExpect(jsonPath("$.paths['/api/users/{id}/enabled'].patch.responses", not(hasKey("401"))))
                // /api/ratings (POST)
                .andExpect(jsonPath("$.paths['/api/ratings'].post.responses", hasKey("403")))
                .andExpect(jsonPath("$.paths['/api/ratings'].post.responses", not(hasKey("401"))))
                // /api/ratings/lodging/{lodgingId}/eligibility (GET)
                .andExpect(jsonPath("$.paths['/api/ratings/lodging/{lodgingId}/eligibility'].get.responses", hasKey("403")))
                .andExpect(jsonPath("$.paths['/api/ratings/lodging/{lodgingId}/eligibility'].get.responses", not(hasKey("401"))))
                // /api/auth/password (POST)
                .andExpect(jsonPath("$.paths['/api/auth/password'].post.responses", hasKey("403")))
                .andExpect(jsonPath("$.paths['/api/auth/password'].post.responses", not(hasKey("401"))));
    }

    @Test
    @DisplayName("OpenAPI reflects 401 on endpoints configured with UNAUTHORIZED entry point")
    void openApiReflectsUnauthorizedOnExplicitEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                // /api/auth/me (GET)
                .andExpect(jsonPath("$.paths['/api/auth/me'].get.responses", hasKey("401")))
                // /api/reservations (POST)
                .andExpect(jsonPath("$.paths['/api/reservations'].post.responses", hasKey("401")))
                // /api/reservations/admin (GET) - both 401 (unauthenticated) and 403 (non-admin)
                .andExpect(jsonPath("$.paths['/api/reservations/admin'].get.responses", hasKey("401")))
                .andExpect(jsonPath("$.paths['/api/reservations/admin'].get.responses", hasKey("403")));
    }
}
