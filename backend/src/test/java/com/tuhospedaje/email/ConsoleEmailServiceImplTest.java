package com.tuhospedaje.email;

import com.tuhospedaje.dto.email.EmailMessage;
import com.tuhospedaje.service.impl.ConsoleEmailServiceImpl;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatNoException;

class ConsoleEmailServiceImplTest {

    private final ConsoleEmailServiceImpl service = new ConsoleEmailServiceImpl();

    @Test
    void sendEmailMessage_doesNotThrow() {
        EmailMessage message = new EmailMessage("guest@test.com", "Subject", "<p>Body</p>", "WELCOME", "7");

        assertThatNoException().isThrownBy(() -> service.send(message));
    }
}
