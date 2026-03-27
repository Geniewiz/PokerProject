package com.example.holdem.table.presentation.ws.outbound;

import com.example.holdem.game.presentation.dto.GameSnapshotResponse;

public record ActionResultMessage(
        String tableId,
        Long userId,
        String action,
        String status,
        GameSnapshotResponse snapshot
) {
}
