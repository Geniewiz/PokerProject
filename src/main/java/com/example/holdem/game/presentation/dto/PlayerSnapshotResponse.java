package com.example.holdem.game.presentation.dto;

public record PlayerSnapshotResponse(
        Long userId,
        int seatNo,
        String position,
        long stack,
        boolean folded
) {
}
