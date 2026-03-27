package com.example.holdem.game.presentation.dto;

public record AvailableActionsResponse(
        boolean canFold,
        boolean canCheck,
        boolean canCall,
        boolean canRaise,
        long callAmount,
        long minimumRaiseToAmount,
        long maximumRaiseToAmount
) {
}
