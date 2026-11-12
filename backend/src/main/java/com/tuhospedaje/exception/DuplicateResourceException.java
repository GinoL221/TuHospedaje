package com.tuhospedaje.exception;

/**
 * Thrown when a create or update would produce a duplicate unique name (Category,
 * Feature, Policy). Carries a message key plus arguments so
 * {@code GlobalExceptionHandler} can resolve a localized message through
 * {@code MessageSource} and answer 409 Conflict, instead of the former hardcoded
 * {@code IllegalArgumentException} text answered as 400.
 */
public class DuplicateResourceException extends RuntimeException {

    private final String errorCode;
    private final Object[] args;

    public DuplicateResourceException(String errorCode, Object... args) {
        super(errorCode);
        this.errorCode = errorCode;
        this.args = args;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Object[] getArgs() {
        return args;
    }
}
