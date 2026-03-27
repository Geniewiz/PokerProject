package com.example.holdem.game.domain;

import com.example.holdem.game.engine.Card;
import java.util.List;

public record CommunityCards(
        List<Card> boardCards,
        int revealedCount
) {
    public List<Card> revealedCards() {
        return boardCards.subList(0, Math.min(revealedCount, boardCards.size()));
    }
}
