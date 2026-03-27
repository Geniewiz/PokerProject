package com.example.holdem.game.application;

import com.example.holdem.game.domain.Hand;
import com.example.holdem.game.engine.HandStateMachine;
import org.springframework.stereotype.Service;

@Service
public class AdvanceStreetUseCase {
    private final GameStateRepository gameStateRepository;
    private final HandStateMachine handStateMachine = new HandStateMachine();

    public AdvanceStreetUseCase(GameStateRepository gameStateRepository) {
        this.gameStateRepository = gameStateRepository;
    }

    public Hand advance(String tableId) {
        Hand hand = gameStateRepository.getRequired(tableId);
        Hand advanced = new Hand(
                hand.handId(),
                hand.tableId(),
                handStateMachine.next(hand.phase()),
                hand.communityCards(),
                hand.pot(),
                hand.playerTurn(),
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
        gameStateRepository.save(advanced);
        return advanced;
    }
}
