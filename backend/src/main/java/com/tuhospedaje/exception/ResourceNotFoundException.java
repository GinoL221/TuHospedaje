package com.tuhospedaje.exception;

public class ResourceNotFoundException extends RuntimeException {

    private final String errorCode;
    private final Object[] args;

    public ResourceNotFoundException(String message) {
        super(message);
        this.errorCode = null;
        this.args = null;
    }

    /**
     * Localizable variant: {@code errorCode} is a {@code messages.properties} key,
     * {@code args} are its interpolation arguments, and {@code fallbackMessage} is used
     * when the exception's {@code getMessage()} is read directly (e.g. logs) instead of
     * being resolved via {@code MessageSource}. Not exercised by any current throw site —
     * added for future callers that want real per-key localization instead of the
     * plain-message fallback.
     */
    public ResourceNotFoundException(String errorCode, Object[] args, String fallbackMessage) {
        super(fallbackMessage);
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
