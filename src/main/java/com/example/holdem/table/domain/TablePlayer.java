package com.example.holdem.table.domain;

public record TablePlayer(
        Long userId,
        String nickname,
        long chipStack,
        boolean connected,
        boolean ready
) {
}
