package com.tuhospedaje.dto.email;

public record EmailMessage(
        String to,
        String subject,
        String htmlBody,
        String emailType,
        String aggregateId) {
}
