package com.example.holdem.table.presentation.ws.outbound;

public record PlayerJoinedMessage(
        String tableId,
        Long userId,
        int seatNo
) {
}
