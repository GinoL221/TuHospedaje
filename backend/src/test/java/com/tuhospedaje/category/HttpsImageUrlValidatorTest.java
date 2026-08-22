package com.tuhospedaje.category;

import com.tuhospedaje.validation.HttpsImageUrlValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Focused, deterministic unit coverage for the category representative-image format
 * check (US-21.1-S3, QA-2-S1/S3). No network access, no Spring context.
 */
class HttpsImageUrlValidatorTest {

    private final HttpsImageUrlValidator validator = new HttpsImageUrlValidator();

    @Test
    void acceptsNullSoItComposesWithASeparateNotBlankConstraintOnCreate() {
        assertThat(validator.isValid(null, null)).isTrue();
    }

    @Test
    void acceptsAnAbsoluteHttpsUrl() {
        assertThat(validator.isValid("https://cdn.tuhospedaje.test/categories/hotel.jpg", null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "   ",
            "not-a-url",
            "ftp://cdn.tuhospedaje.test/image.jpg",
            "http://cdn.tuhospedaje.test/image.jpg",
            "javascript:alert(1)",
            "data:image/png;base64,AAAA",
            "//cdn.tuhospedaje.test/image.jpg",
            "/relative/image.jpg"
    })
    void rejectsBlankMalformedAndUnsupportedSchemeValues(String candidate) {
        assertThat(validator.isValid(candidate, null)).isFalse();
    }

    @Test
    void rejectsValuesLongerThanTheMaximumAllowedLength() {
        String longUrl = "https://cdn.tuhospedaje.test/" + "a".repeat(2050) + ".jpg";
        assertThat(validator.isValid(longUrl, null)).isFalse();
    }
}
