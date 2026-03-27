package com.example.holdem.table.presentation.rest.dto;

public record TablePlayerResponse(
        Long userId,
        String nickname,
        long chipStack,
        boolean connected,
        boolean ready
) {
}
