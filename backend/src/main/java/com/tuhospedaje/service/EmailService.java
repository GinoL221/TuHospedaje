package com.tuhospedaje.service;

import com.tuhospedaje.dto.auth.RegisterRequest;

public interface EmailService {
    void sendWelcomeEmail(RegisterRequest request);
}
