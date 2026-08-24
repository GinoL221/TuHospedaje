package com.tuhospedaje.service;

public final class EmailTransportFailure extends RuntimeException {

    private final EmailTransportFailureClassification classification;

    public EmailTransportFailure(EmailTransportFailureClassification classification) {
        super(classification.name());
        this.classification = classification;
    }

    public EmailTransportFailureClassification classification() {
        return classification;
    }
}
