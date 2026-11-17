package com.tuhospedaje.exception;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ensures {@code messages.properties} and {@code messages_es.properties} stay in parity:
 * identical key sets, matching {@code {N}} placeholder arguments, and all service-layer
 * localization keys present and non-blank in both bundles.
 */
class MessageBundleParityTest {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{(\\d+)}");

    private static final List<String> REQUIRED_SERVICE_KEYS = List.of(
            "error.auth.invalid",
            "error.auth.duplicate_email",
            "error.user.not_found",
            "error.auth.current_password.incorrect",
            "error.reservation.cannot_cancel",
            "error.reservation.cancel_deadline_passed",
            "error.reservation.not_found",
            "error.reservation.lodging_not_found",
            "error.reservation.not_available",
            "error.sort.invalid_field",
            "error.sort.invalid_direction"
    );

    @Test
    void englishAndSpanishBundlesHaveIdenticalKeysAndPlaceholders() throws IOException {
        Properties en = loadProperties("messages.properties");
        Properties es = loadProperties("messages_es.properties");

        assertThat(en.stringPropertyNames())
                .as("messages.properties and messages_es.properties must declare the exact same keys")
                .containsExactlyInAnyOrderElementsOf(es.stringPropertyNames());

        for (String key : en.stringPropertyNames()) {
            String enVal = en.getProperty(key);
            String esVal = es.getProperty(key);
            assertThat(enVal).as("Blank English value for %s", key).isNotBlank();
            assertThat(esVal).as("Blank Spanish value for %s", key).isNotBlank();
            assertThat(placeholders(enVal))
                    .as("Placeholder mismatch for key %s", key)
                    .isEqualTo(placeholders(esVal));
        }
    }

    @Test
    void bothBundlesContainAllServiceLocalizationKeys() throws IOException {
        Properties en = loadProperties("messages.properties");
        Properties es = loadProperties("messages_es.properties");

        assertThat(en.stringPropertyNames()).containsAll(REQUIRED_SERVICE_KEYS);
        assertThat(es.stringPropertyNames()).containsAll(REQUIRED_SERVICE_KEYS);
    }

    private Properties loadProperties(String filename) throws IOException {
        Properties props = new Properties();
        ClassPathResource resource = new ClassPathResource(filename);
        try (InputStreamReader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            props.load(reader);
        }
        return props;
    }

    private Set<String> placeholders(String text) {
        Set<String> found = new HashSet<>();
        Matcher matcher = PLACEHOLDER.matcher(text);
        while (matcher.find()) {
            found.add(matcher.group(1));
        }
        return found;
    }
}
