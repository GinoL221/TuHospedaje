package com.tuhospedaje.service;

import com.tuhospedaje.dto.email.EmailMessage;

public interface EmailTransport {

    void submit(EmailMessage message);
}
