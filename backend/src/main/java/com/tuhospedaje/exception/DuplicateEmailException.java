package com.tuhospedaje.exception;

/**
 * Thrown by {@code AuthServiceImpl.register} when the requested email already belongs
 * to an existing account. Kept distinct from the generic {@link IllegalArgumentException}
 * handler so the response body can carry a stable {@code code} the frontend discriminates
 * on, instead of parsing the localized error message text.
 */
public class DuplicateEmailException extends RuntimeException {

    public static final String ERROR_CODE = "duplicate_email";

    public DuplicateEmailException(String message) {
        super(message);
    }
}
