package com.tuhospedaje.email.outbox;

import com.tuhospedaje.dto.email.EmailMessage;
import com.tuhospedaje.service.EmailTransportFailure;
import com.tuhospedaje.service.EmailTransportFailureClassification;
import com.tuhospedaje.service.impl.SmtpEmailServiceImpl;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SmtpEmailServiceImplTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final SmtpEmailServiceImpl service = new SmtpEmailServiceImpl(mailSender);

    @Test
    void submitsTheExactStoredPayloadAsUtf8Html() throws Exception {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        EmailMessage stored = new EmailMessage(
                "ana@example.com", "Stored subject", "<p>Stored body</p>", "WELCOME", "42");

        service.submit(stored);

        assertThat(mimeMessage.getAllRecipients()[0].toString()).isEqualTo("ana@example.com");
        assertThat(mimeMessage.getSubject()).isEqualTo("Stored subject");
        assertThat(mimeMessage.getContent()).isEqualTo("<p>Stored body</p>");
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void rejectsBlankStoredPayloadWithoutSubmitting() {
        EmailMessage stored = new EmailMessage("ana@example.com", "", "<p>Stored body</p>", "WELCOME", "42");

        assertThatThrownBy(() -> service.submit(stored))
                .isInstanceOf(EmailTransportFailure.class)
                .extracting(error -> ((EmailTransportFailure) error).classification())
                .isEqualTo(EmailTransportFailureClassification.INVALID_STORED_PAYLOAD);

        verify(mailSender, never()).createMimeMessage();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void classifiesAuthenticationRejectionWithoutExposingProviderText() {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        org.mockito.Mockito.doThrow(new MailAuthenticationException("provider secret"))
                .when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> service.submit(new EmailMessage(
                "ana@example.com", "Stored subject", "<p>Stored body</p>", "WELCOME", "42")))
                .isInstanceOf(EmailTransportFailure.class)
                .extracting(error -> ((EmailTransportFailure) error).classification())
                .isEqualTo(EmailTransportFailureClassification.SMTP_AUTHENTICATION_REJECTED);
    }
}
