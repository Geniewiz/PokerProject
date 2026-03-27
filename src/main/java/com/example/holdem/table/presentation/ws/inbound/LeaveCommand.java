package com.example.holdem.table.presentation.ws.inbound;

public record LeaveCommand(
        String tableId,
        Long userId
) {
}
