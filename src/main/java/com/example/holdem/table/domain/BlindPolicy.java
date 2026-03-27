package com.example.holdem.table.domain;

public record BlindPolicy(
        long smallBlind,
        long bigBlind,
        long ante
) {
}
