package com.example.holdem.table.presentation.rest.dto;

import java.util.List;

public record TableSnapshotResponse(
        String tableId,
        String name,
        String status,
        long smallBlind,
        long bigBlind,
        Integer dealerSeatNo,
        int seatedPlayerCount,
        List<SeatResponse> seats
) {
}
