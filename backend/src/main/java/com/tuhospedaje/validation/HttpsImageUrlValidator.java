package com.tuhospedaje.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Validates that a representative-image value, when present, is an absolute
 * {@code https} URL no longer than 2048 characters. {@code null} is always accepted so
 * this constraint composes with {@code @NotBlank} for a create-only requirement: an
 * update that omits the field (null) preserves legacy data, while any non-null value
 * — blank, malformed, or a non-https scheme such as {@code javascript:} or
 * {@code data:} — is rejected. No remote fetch or content-type inspection is
 * performed (avoids SSRF and provider coupling).
 */
public class HttpsImageUrlValidator implements ConstraintValidator<HttpsImageUrl, String> {

    private static final int MAX_LENGTH = 2048;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        if (value.isBlank() || value.length() > MAX_LENGTH) {
            return false;
        }
        try {
            URI uri = new URI(value);
            return uri.isAbsolute()
                    && "https".equalsIgnoreCase(uri.getScheme())
                    && uri.getHost() != null
                    && !uri.getHost().isBlank();
        } catch (URISyntaxException exception) {
            return false;
        }
    }
}
