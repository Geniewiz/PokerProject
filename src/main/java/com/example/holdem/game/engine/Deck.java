package com.example.holdem.game.engine;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

public class Deck {
    private final Deque<Card> cards;

    public Deck() {
        List<Card> shuffled = new ArrayList<>();
        for (Card.Suit suit : Card.Suit.values()) {
            for (Card.Rank rank : Card.Rank.values()) {
                shuffled.add(new Card(suit, rank));
            }
        }
        Collections.shuffle(shuffled);
        this.cards = new ArrayDeque<>(shuffled);
    }

    public Card draw() {
        return cards.pop();
    }
}
