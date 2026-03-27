package com.example.holdem.game.domain;

public record PlayerTurn(
        Long userId,
        long callAmount,
        long minimumRaiseToAmount
) {
}
