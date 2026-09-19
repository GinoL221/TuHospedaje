package com.tuhospedaje.service.impl;

import com.tuhospedaje.dto.email.EmailMessage;
import com.tuhospedaje.service.EmailTransport;
import com.tuhospedaje.service.EmailTransportFailure;
import com.tuhospedaje.service.EmailTransportFailureClassification;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Primary
@ConditionalOnProperty(name = "app.mail.smtp.enabled", havingValue = "true")
public class SmtpEmailServiceImpl implements EmailTransport {

    private final JavaMailSender mailSender;

    public SmtpEmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void submit(EmailMessage message) {
        validateStoredPayload(message);
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
            helper.setTo(message.to());
            helper.setSubject(message.subject());
            helper.setText(message.htmlBody(), true);
            mailSender.send(mimeMessage);
        } catch (MailAuthenticationException exception) {
            throw new EmailTransportFailure(EmailTransportFailureClassification.SMTP_AUTHENTICATION_REJECTED);
        } catch (MailException exception) {
            throw new EmailTransportFailure(EmailTransportFailureClassification.SMTP_UNAVAILABLE);
        } catch (MessagingException exception) {
            throw new EmailTransportFailure(EmailTransportFailureClassification.INVALID_STORED_PAYLOAD);
        }
    }

    private void validateStoredPayload(EmailMessage message) {
        if (message == null || isBlankOrTooLong(message.to(), 256)
                || isBlankOrTooLong(message.subject(), 256) || isBlankOrTooLong(message.htmlBody(), 65535)) {
            throw new EmailTransportFailure(EmailTransportFailureClassification.INVALID_STORED_PAYLOAD);
        }
        try {
            InternetAddress address = new InternetAddress(message.to(), true);
            address.validate();
        } catch (AddressException exception) {
            throw new EmailTransportFailure(EmailTransportFailureClassification.INVALID_STORED_PAYLOAD);
        }
    }

    private boolean isBlankOrTooLong(String value, int maximumLength) {
        return value == null || value.isBlank() || value.length() > maximumLength;
    }
}
