package com.tuhospedaje.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class RequestBodyLimitPropertyTest {

    @Value("${server.tomcat.max-swallow-size}")
    private String maxSwallowSize;

    @Value("${server.tomcat.max-http-form-post-size}")
    private String maxHttpFormPostSize;

    @Value("${spring.servlet.multipart.max-file-size}")
    private String maxFileSize;

    @Value("${spring.servlet.multipart.max-request-size}")
    private String maxRequestSize;

    @Test
    void requestBodyLimitsAreConfigured() {
        assertThat(maxSwallowSize).isEqualTo("6MB");
        assertThat(maxHttpFormPostSize).isEqualTo("2MB");
        assertThat(maxFileSize).isEqualTo("5MB");
        assertThat(maxRequestSize).isEqualTo("6MB");
    }
}
