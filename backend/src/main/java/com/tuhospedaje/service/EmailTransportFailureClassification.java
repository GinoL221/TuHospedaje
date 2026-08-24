package com.tuhospedaje.service;

public enum EmailTransportFailureClassification {
    INVALID_STORED_PAYLOAD(false),
    SMTP_AUTHENTICATION_REJECTED(false),
    SMTP_UNAVAILABLE(true),
    SMTP_UNKNOWN(true);

    private final boolean retryable;

    EmailTransportFailureClassification(boolean retryable) {
        this.retryable = retryable;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
