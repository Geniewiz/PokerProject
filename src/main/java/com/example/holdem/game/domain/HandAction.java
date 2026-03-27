package com.example.holdem.game.domain;

public record HandAction(
        Long userId,
        HandPhase phase,
        PlayerActionType actionType,
        long amount
) {
}
