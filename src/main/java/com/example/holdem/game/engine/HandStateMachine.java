package com.example.holdem.game.engine;

import com.example.holdem.game.domain.HandPhase;

public class HandStateMachine {

    public HandPhase next(HandPhase current) {
        return switch (current) {
            case PREFLOP -> HandPhase.FLOP;
            case FLOP -> HandPhase.TURN;
            case TURN -> HandPhase.RIVER;
            case RIVER -> HandPhase.SHOWDOWN;
            case WAITING, SHOWDOWN, FINISHED -> HandPhase.FINISHED;
        };
    }
}
