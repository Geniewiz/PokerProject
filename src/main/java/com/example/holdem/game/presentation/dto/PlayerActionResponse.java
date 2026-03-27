package com.example.holdem.game.presentation.dto;

public record PlayerActionResponse(
        Long userId,
        String phase,
        String action,
        long amount
) {
}
