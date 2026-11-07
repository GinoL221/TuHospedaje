package com.tuhospedaje.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class JwtSecretValidationTest {

    /** 32 zero bytes, Base64. Test fixture only. */
    public static final String VALID_TEST_SECRET = "A".repeat(43) + "=";

    private final JwtService jwtService = new JwtService();

    @Test
    void shouldRejectBlankSecretKey() {
        ReflectionTestUtils.setField(jwtService, "secretKey", "   ");
        ReflectionTestUtils.setField(jwtService, "environment", new MockEnvironment());

        assertThatThrownBy(jwtService::validateSecretKey)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("blank");
    }

    @Test
    void shouldRejectInvalidBase64SecretKey() {
        ReflectionTestUtils.setField(jwtService, "secretKey", "!!!not-base64!!!");
        ReflectionTestUtils.setField(jwtService, "environment", new MockEnvironment());

        assertThatThrownBy(jwtService::validateSecretKey)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Base64");
    }

    @Test
    void shouldRejectSecretKeyShorterThan256Bits() {
        // "c2hvcnQ=" decodes to "short" (5 bytes < 32 bytes)
        ReflectionTestUtils.setField(jwtService, "secretKey", "c2hvcnQ=");
        ReflectionTestUtils.setField(jwtService, "environment", new MockEnvironment());

        assertThatThrownBy(jwtService::validateSecretKey)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("256 bits");
    }

    @Test
    void shouldRejectPlaceholderSecretInProductionProfile() {
        MockEnvironment prodEnv = new MockEnvironment();
        prodEnv.setActiveProfiles("prod");

        // 32+ bytes base64 but containing change-me
        ReflectionTestUtils.setField(jwtService, "secretKey", Base64.getEncoder().encodeToString(
                "change-me-generate-with-openssl-rand-base64-48".getBytes(StandardCharsets.UTF_8)));
        ReflectionTestUtils.setField(jwtService, "environment", prodEnv);

        assertThatThrownBy(jwtService::validateSecretKey)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Production environment cannot use default or placeholder JWT secret");
    }

    @Test
    void shouldAcceptValidSecretKeyInDev() {
        MockEnvironment devEnv = new MockEnvironment();
        devEnv.setActiveProfiles("dev");

        ReflectionTestUtils.setField(jwtService, "secretKey", VALID_TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "environment", devEnv);

        assertThatCode(jwtService::validateSecretKey).doesNotThrowAnyException();
    }
}
