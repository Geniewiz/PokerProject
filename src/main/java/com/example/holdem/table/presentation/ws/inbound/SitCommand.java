package com.example.holdem.table.presentation.ws.inbound;

public record SitCommand(
        String tableId,
        Long userId,
        int seatNo,
        long buyInAmount
) {
}
