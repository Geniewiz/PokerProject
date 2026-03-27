package com.example.holdem.game.engine;

public record HoleCards(
        Card first,
        Card second
) {
    public java.util.List<Card> asList() {
        return java.util.List.of(first, second);
    }
}
