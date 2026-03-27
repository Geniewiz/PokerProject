package com.example.holdem.auth.presentation.dto;

public record LoginResponse(
        Long userId,
        String username,
        String accessToken
) {
}
