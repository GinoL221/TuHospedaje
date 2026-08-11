package com.tuhospedaje.email;

import com.tuhospedaje.dto.email.EmailMessage;
import com.tuhospedaje.service.impl.ConsoleEmailServiceImpl;
import com.tuhospedaje.service.impl.SmtpEmailServiceImpl;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceTransportTest {

    @Mock
    private JavaMailSender mailSender;

    @Test
    void smtpMapsAndSendsTheEmailMessageFields() throws Exception {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        EmailMessage message = new EmailMessage(
                "guest@example.com", "Subject", "<p>Body</p>", "WELCOME", "7");

        new SmtpEmailServiceImpl(mailSender).send(message);

        verify(mailSender).send(mimeMessage);
        assertThat(mimeMessage.getRecipients(Message.RecipientType.TO)[0].toString())
                .isEqualTo("guest@example.com");
        assertThat(mimeMessage.getSubject()).isEqualTo("Subject");
        assertThat(mimeMessage.getContent().toString()).contains("<p>Body</p>");
    }

    @Test
    void smtpDeliveryFailurePropagatesToTheWorkerBoundary() {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new org.springframework.mail.MailSendException("smtp down"))
                .when(mailSender).send(mimeMessage);

        assertThatThrownBy(() -> new SmtpEmailServiceImpl(mailSender).send(
                new EmailMessage("guest@example.com", "Subject", "<p>Body</p>", "WELCOME", "7")))
                .isInstanceOf(org.springframework.mail.MailSendException.class);
    }

    @Test
    void consoleAcceptsTheTransportContract() {
        EmailMessage message = new EmailMessage(
                "guest@example.com", "Subject", "<p>Body</p>", "WELCOME", "7");

        new ConsoleEmailServiceImpl().send(message);
    }
}
