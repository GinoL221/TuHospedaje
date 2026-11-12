package com.tuhospedaje.exception;

/**
 * {@link IllegalArgumentException} carrying a message key plus arguments, so
 * {@code GlobalExceptionHandler} can resolve a localized message through
 * {@code MessageSource} while the status stays 400 (business rules that are not
 * conflicts, e.g. deleting a category that has lodgings). Plain
 * {@code IllegalArgumentException} sites with literal messages keep the existing
 * verbatim-fallback behavior — see {@code handleIllegalArgument}.
 */
public class LocalizedIllegalArgumentException extends IllegalArgumentException {

    private final String errorCode;
    private final Object[] args;

    public LocalizedIllegalArgumentException(String errorCode, Object... args) {
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
