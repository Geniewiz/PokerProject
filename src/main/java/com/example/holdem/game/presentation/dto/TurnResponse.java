package com.example.holdem.game.presentation.dto;

public record TurnResponse(
        Long userId,
        long callAmount,
        long minimumRaiseToAmount
) {
}
