package com.example.holdem.game.domain;

import java.util.Set;

public record SidePot(
        long amount,
        Set<Long> eligiblePlayerIds
) {
}
