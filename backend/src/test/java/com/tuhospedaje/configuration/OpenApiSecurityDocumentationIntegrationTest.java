package com.tuhospedaje.configuration;

import com.tuhospedaje.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
// Sentinel version proves /v3/api-docs takes info.version from the app.api-version
// property (OpenApiConfig bean), not from a Java literal.
@TestPropertySource(properties = "app.api-version=9.9.9-doc-test")
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

    @Test
    @DisplayName("OpenAPI info.version comes from the app.api-version property")
    void openApiVersionComesFromProperty() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.version", equalTo("9.9.9-doc-test")));
    }

    @Test
    @DisplayName("OpenAPI error responses reference the shared ApiError schema")
    void openApiErrorResponsesReferenceApiErrorSchema() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.schemas", hasKey("ApiError")))
                // sample of transformed statuses across controllers
                .andExpect(jsonPath("$.paths['/api/auth/login'].post.responses['400']"
                        + ".content['*/*'].schema.$ref", equalTo("#/components/schemas/ApiError")))
                .andExpect(jsonPath("$.paths['/api/lodgings/{id}'].get.responses['404']"
                        + ".content['*/*'].schema.$ref", equalTo("#/components/schemas/ApiError")))
                .andExpect(jsonPath("$.paths['/api/upload'].post.responses['413']"
                        + ".content['*/*'].schema.$ref", equalTo("#/components/schemas/ApiError")))
                .andExpect(jsonPath("$.paths['/api/auth/refresh'].post.responses['429']"
                        + ".content['*/*'].schema.$ref", equalTo("#/components/schemas/ApiError")));
    }

    @Test
    @DisplayName("OpenAPI documents the two previously unannotated handlers")
    void openApiDocumentsResendWelcomeAndRecommendations() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                // POST /api/auth/welcome-email/resend
                .andExpect(jsonPath("$.paths['/api/auth/welcome-email/resend'].post.responses",
                        hasKey("202")))
                .andExpect(jsonPath("$.paths['/api/auth/welcome-email/resend'].post.responses",
                        hasKey("429")))
                .andExpect(jsonPath("$.paths['/api/auth/welcome-email/resend'].post.responses['200']"
                        + ".content['*/*'].schema.type", equalTo("object")))
                // CSRF-exempt: no csrfToken security requirement on this POST
                .andExpect(jsonPath("$.paths['/api/auth/welcome-email/resend'].post", not(hasKey("security"))))
                // GET /api/lodgings/recommendations
                .andExpect(jsonPath("$.paths['/api/lodgings/recommendations'].get.responses", hasKey("400")))
                .andExpect(jsonPath("$.paths['/api/lodgings/recommendations'].get.responses['400']"
                        + ".content['*/*'].schema.$ref", equalTo("#/components/schemas/ApiError")));
    }

    @Test
    @DisplayName("OpenAPI no longer requires csrfToken on GET handlers")
    void openApiDropsCsrfTokenFromGetHandlers() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/favorites'].get", not(hasKey("security"))))
                .andExpect(jsonPath("$.paths['/api/reservations/{id}'].get", not(hasKey("security"))))
                .andExpect(jsonPath("$.paths['/api/reservations/my'].get", not(hasKey("security"))))
                .andExpect(jsonPath("$.paths['/api/reservations/admin'].get", not(hasKey("security"))))
                .andExpect(jsonPath("$.paths['/api/lodgings/admin'].get", not(hasKey("security"))))
                .andExpect(jsonPath("$.paths['/api/users'].get", not(hasKey("security"))))
                // unsafe methods keep it: the schemas still carry csrfToken
                .andExpect(content().string(containsString("csrfToken")));
    }
}
