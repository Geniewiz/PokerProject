package com.example.holdem.game.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ShowdownResolver {
    private final HandEvaluator handEvaluator = new HandEvaluator();

    public List<Long> resolveWinners(Map<Long, HoleCards> holeCardsByPlayer, List<Card> boardCards) {
        List<Long> winners = new ArrayList<>();
        long bestScore = Long.MIN_VALUE;
        for (Map.Entry<Long, HoleCards> entry : holeCardsByPlayer.entrySet()) {
            List<Card> cards = new ArrayList<>(boardCards);
            cards.addAll(entry.getValue().asList());
            long score = handEvaluator.evaluate(cards);
            if (score > bestScore) {
                bestScore = score;
                winners.clear();
                winners.add(entry.getKey());
            } else if (score == bestScore) {
                winners.add(entry.getKey());
            }
        }
        return List.copyOf(winners);
    }

    public List<Long> resolveWinnersForEligiblePlayers(
            Map<Long, HoleCards> holeCardsByPlayer,
            List<Card> boardCards,
            Set<Long> eligiblePlayerIds
    ) {
        Map<Long, HoleCards> filtered = new java.util.HashMap<>();
        for (Map.Entry<Long, HoleCards> entry : holeCardsByPlayer.entrySet()) {
            if (eligiblePlayerIds.contains(entry.getKey())) {
                filtered.put(entry.getKey(), entry.getValue());
            }
        }
        return resolveWinners(filtered, boardCards);
    }
}
