package com.example.holdem.table.presentation.ws.inbound;

public record ActionCommand(
        String tableId,
        Long userId,
        String action,
        long amount
) {
}
