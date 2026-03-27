package com.example.holdem.game.domain;

import com.example.holdem.game.engine.HoleCards;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record Hand(
        String handId,
        String tableId,
        HandPhase phase,
        CommunityCards communityCards,
        Pot pot,
        PlayerTurn playerTurn,
        Map<Long, Long> committedChips,
        Map<Long, Long> streetCommittedChips,
        Map<Long, Long> stackByPlayer,
        Map<Long, HoleCards> holeCardsByPlayer,
        Map<Long, TablePlayerSnapshot> players,
        List<Long> turnOrder,
        List<Long> pendingPlayerIds,
        Set<Long> foldedPlayerIds,
        Set<Long> allInPlayerIds,
        List<HandAction> actions,
        long currentBet,
        long bigBlind,
        Long dealerUserId,
        Long winnerUserId
) {
    public boolean isFinished() {
        return phase == HandPhase.FINISHED;
    }
}
