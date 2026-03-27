package com.example.holdem.lobby.domain;

public record TableSummary(
        String tableId,
        String name,
        int maxPlayers,
        int seatedPlayers,
        long smallBlind,
        long bigBlind
) {
}
