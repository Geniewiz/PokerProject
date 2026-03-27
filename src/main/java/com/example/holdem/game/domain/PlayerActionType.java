package com.example.holdem.game.domain;

public enum PlayerActionType {
    FOLD,
    CHECK,
    CALL,
    RAISE;

    public static PlayerActionType from(String value) {
        return PlayerActionType.valueOf(value.toUpperCase());
    }
}
