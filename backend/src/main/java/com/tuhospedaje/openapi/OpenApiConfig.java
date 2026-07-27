package com.tuhospedaje.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TuHospedaje API")
                        .version("1.0")
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
