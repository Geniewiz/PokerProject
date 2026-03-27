package com.example.holdem.game.engine;

import com.example.holdem.game.domain.Hand;
import com.example.holdem.game.domain.PlayerActionType;

public class ActionValidator {

    public boolean canAct(Hand hand, Long userId, PlayerActionType actionType, long amount) {
        if (hand.playerTurn() == null || !hand.playerTurn().userId().equals(userId)) {
            return false;
        }
        if (hand.foldedPlayerIds().contains(userId) || hand.allInPlayerIds().contains(userId) || hand.isFinished()) {
            return false;
        }

        long stack = hand.stackByPlayer().getOrDefault(userId, 0L);
        long callAmount = hand.playerTurn().callAmount();
        long minimumRaiseToAmount = hand.playerTurn().minimumRaiseToAmount();

        return switch (actionType) {
            case FOLD -> true;
            case CHECK -> callAmount == 0 && amount == 0;
            case CALL -> {
                long required = Math.min(callAmount, stack);
                yield callAmount > 0 && amount == required && amount > 0;
            }
            case RAISE -> {
                boolean regularRaise = amount >= minimumRaiseToAmount && amount <= stack;
                boolean allInRaise = amount == stack && amount > callAmount;
                yield regularRaise || allInRaise;
            }
        };
    }
}
