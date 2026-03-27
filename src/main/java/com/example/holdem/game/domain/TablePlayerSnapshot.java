package com.example.holdem.game.domain;

public record TablePlayerSnapshot(
        Long userId,
        int seatNo,
        String position,
        long stack
) {
}
