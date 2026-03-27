package com.example.holdem.game.application;

import com.example.holdem.game.domain.Hand;
import com.example.holdem.game.domain.HandPhase;
import org.springframework.stereotype.Service;

@Service
public class FinishHandUseCase {
    private final GameStateRepository gameStateRepository;

    public FinishHandUseCase(GameStateRepository gameStateRepository) {
        this.gameStateRepository = gameStateRepository;
    }

    public Hand finish(String tableId) {
        Hand hand = gameStateRepository.getRequired(tableId);
        Hand finished = new Hand(
                hand.handId(),
                hand.tableId(),
                HandPhase.FINISHED,
                hand.communityCards(),
                hand.pot(),
                null,
                hand.committedChips(),
                hand.streetCommittedChips(),
                hand.stackByPlayer(),
                hand.holeCardsByPlayer(),
                hand.players(),
                hand.turnOrder(),
                hand.pendingPlayerIds(),
                hand.foldedPlayerIds(),
                hand.allInPlayerIds(),
                hand.actions(),
                hand.currentBet(),
                hand.bigBlind(),
                hand.dealerUserId(),
                hand.winnerUserId()
        );
        gameStateRepository.save(finished);
        return finished;
    }
}
