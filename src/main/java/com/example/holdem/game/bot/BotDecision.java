package com.example.holdem.game.bot;

import com.example.holdem.game.domain.PlayerActionType;

public record BotDecision(
        PlayerActionType actionType,
        long amount
) {
}
