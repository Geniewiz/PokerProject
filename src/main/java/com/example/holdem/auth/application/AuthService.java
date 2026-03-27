package com.example.holdem.auth.application;

import com.example.holdem.auth.presentation.dto.LoginRequest;
import com.example.holdem.auth.presentation.dto.LoginResponse;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final SessionService sessionService;

    public AuthService(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    public LoginResponse login(LoginRequest request) {
        String token = sessionService.issueSessionToken(request.username());
        return new LoginResponse(1L, request.username(), token);
    }
}
