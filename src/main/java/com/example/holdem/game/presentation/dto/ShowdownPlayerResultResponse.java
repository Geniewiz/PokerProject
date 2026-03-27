package com.example.holdem.game.presentation.dto;

import java.util.List;

public record ShowdownPlayerResultResponse(
        Long userId,
        List<String> holeCards,
        String handRank,
        boolean winner
) {
}
