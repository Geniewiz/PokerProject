package com.example.holdem.game.presentation.dto;

import java.util.List;
import java.util.Map;

public record GameSnapshotResponse(
        String tableId,
        String handId,
        String phase,
        boolean finished,
        Long dealerUserId,
        Long winnerUserId,
        long mainPot,
        long currentBet,
        List<String> communityCards,
        TurnResponse turn,
        AvailableActionsResponse availableActions,
        Map<Long, PlayerSnapshotResponse> players,
        List<PlayerActionResponse> recentActions,
        List<String> myHoleCards,
        List<ShowdownPlayerResultResponse> showdownResults
) {
}
