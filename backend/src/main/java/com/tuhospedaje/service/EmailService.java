package com.tuhospedaje.service;

import com.tuhospedaje.dto.email.EmailMessage;

public interface EmailService {

    void send(EmailMessage message);
}
