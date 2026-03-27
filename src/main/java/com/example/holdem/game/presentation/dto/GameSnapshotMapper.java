package com.example.holdem.game.presentation.dto;

import com.example.holdem.game.domain.Hand;
import com.example.holdem.game.domain.PlayerTurn;
import com.example.holdem.game.engine.Card;
import com.example.holdem.game.engine.HandEvaluator;
import com.example.holdem.game.engine.HoleCards;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class GameSnapshotMapper {
    private final HandEvaluator handEvaluator = new HandEvaluator();

    public GameSnapshotResponse from(Hand hand, Long requesterUserId) {
        Map<Long, PlayerSnapshotResponse> players = new LinkedHashMap<>();
        hand.players().forEach((userId, player) -> players.put(
                userId,
                new PlayerSnapshotResponse(
                        userId,
                        player.seatNo(),
                        player.position(),
                        player.stack(),
                        hand.foldedPlayerIds().contains(userId)
                )
        ));

        return new GameSnapshotResponse(
                hand.tableId(),
                hand.handId(),
                hand.phase().name(),
                hand.isFinished(),
                hand.dealerUserId(),
                hand.winnerUserId(),
                hand.pot().mainPot(),
                hand.currentBet(),
                hand.communityCards().revealedCards().stream().map(Object::toString).toList(),
                toTurn(hand.playerTurn()),
                toAvailableActions(hand, requesterUserId),
                Map.copyOf(players),
                hand.actions().stream()
                        .map(action -> new PlayerActionResponse(
                                action.userId(),
                                action.phase().name(),
                                action.actionType().name(),
                                action.amount()
                        ))
                        .toList(),
                toMyHoleCards(hand, requesterUserId),
                toShowdownResults(hand)
        );
    }

    private TurnResponse toTurn(PlayerTurn playerTurn) {
        if (playerTurn == null) {
            return null;
        }
        return new TurnResponse(playerTurn.userId(), playerTurn.callAmount(), playerTurn.minimumRaiseToAmount());
    }

    private AvailableActionsResponse toAvailableActions(Hand hand, Long requesterUserId) {
        if (requesterUserId == null || hand.playerTurn() == null || hand.isFinished()) {
            return null;
        }
        boolean myTurn = requesterUserId.equals(hand.playerTurn().userId());
        long stack = hand.stackByPlayer().getOrDefault(requesterUserId, 0L);
        long requiredCallAmount = hand.playerTurn().callAmount();
        long payableCallAmount = Math.min(requiredCallAmount, stack);
        long minRaise = hand.playerTurn().minimumRaiseToAmount();
        boolean folded = hand.foldedPlayerIds().contains(requesterUserId);

        if (!myTurn || folded) {
            return new AvailableActionsResponse(false, false, false, false, payableCallAmount, minRaise, stack);
        }
        return new AvailableActionsResponse(
                true,
                requiredCallAmount == 0,
                requiredCallAmount > 0 && stack > 0,
                stack >= minRaise,
                payableCallAmount,
                minRaise,
                stack
        );
    }

    private List<String> toMyHoleCards(Hand hand, Long requesterUserId) {
        if (requesterUserId == null) {
            return List.of();
        }
        HoleCards cards = hand.holeCardsByPlayer().get(requesterUserId);
        if (cards == null) {
            return Collections.emptyList();
        }
        return List.of(cards.first().toString(), cards.second().toString());
    }

    private List<ShowdownPlayerResultResponse> toShowdownResults(Hand hand) {
        if (!hand.isFinished()) {
            return List.of();
        }
        List<Card> boardCards = hand.communityCards().boardCards();
        if (boardCards.size() < 5) {
            return List.of();
        }

        List<ShowdownCandidate> candidates = new ArrayList<>();
        for (Map.Entry<Long, HoleCards> entry : hand.holeCardsByPlayer().entrySet()) {
            Long userId = entry.getKey();
            if (hand.foldedPlayerIds().contains(userId)) {
                continue;
            }
            List<Card> cards = new ArrayList<>(boardCards);
            cards.addAll(entry.getValue().asList());
            HandEvaluator.EvaluatedHand evaluatedHand = handEvaluator.evaluateWithRank(cards);
            candidates.add(new ShowdownCandidate(userId, entry.getValue(), evaluatedHand.score(), evaluatedHand.handRank()));
        }
        if (candidates.isEmpty()) {
            return List.of();
        }

        long bestScore = candidates.stream().mapToLong(ShowdownCandidate::score).max().orElse(Long.MIN_VALUE);
        return candidates.stream()
                .map(candidate -> new ShowdownPlayerResultResponse(
                        candidate.userId(),
                        List.of(candidate.holeCards().first().toString(), candidate.holeCards().second().toString()),
                        candidate.handRank(),
                        candidate.score() == bestScore
                ))
                .toList();
    }

    private record ShowdownCandidate(Long userId, HoleCards holeCards, long score, String handRank) {
    }
}
