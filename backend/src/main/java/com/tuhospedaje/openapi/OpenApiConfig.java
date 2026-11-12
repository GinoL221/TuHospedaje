package com.tuhospedaje.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private final String apiVersion;

    /**
     * Reads the release version from the {@code app.api-version} property — declared in
     * {@code application.properties} (production) and in the test tree's replacement
     * {@code src/test/resources/application.properties}, both pinned to the pom.xml
     * version by {@code tools/tests/test_repo_hygiene.py} — so bumping the release never
     * requires editing this file. No fallback: a context that fails to supply the
     * property must fail startup rather than silently publish a stale version.
     */
    public OpenApiConfig(@Value("${app.api-version}") String apiVersion) {
        this.apiVersion = apiVersion;
    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TuHospedaje API")
                        .version(apiVersion)
                        .description("API de reservas de alojamientos - Proyecto Digital House"))
                .components(new Components()
                        .addSecuritySchemes("csrfToken", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-XSRF-TOKEN")
                                .description("Authentication uses an httpOnly ACCESS_TOKEN cookie set by "
                                        + "/api/auth/login, not a bearer token — it is sent automatically by "
                                        + "the browser and cannot be supplied here. This scheme documents the "
                                        + "CSRF token required on unsafe requests: read it from the XSRF-TOKEN "
                                        + "cookie (set after GET /api/auth/csrf) and echo it in this header.")));
    }
}
