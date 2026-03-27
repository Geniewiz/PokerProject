package com.example.holdem.game.domain;

import java.util.List;

public record Pot(
        long mainPot,
        List<SidePot> sidePots
) {
}
