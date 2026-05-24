package com.tuhospedaje.service;

import com.tuhospedaje.dto.auth.AuthResponse;
import com.tuhospedaje.dto.auth.LoginRequest;
import com.tuhospedaje.dto.auth.RegisterRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}
