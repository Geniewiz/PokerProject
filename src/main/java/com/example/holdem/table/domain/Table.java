package com.example.holdem.table.domain;

import java.util.List;

public record Table(
        String tableId,
        String name,
        TableStatus status,
        BlindPolicy blindPolicy,
        List<Seat> seats,
        Integer dealerSeatNo
) {
    public int seatedPlayerCount() {
        return (int) seats.stream().filter(Seat::isOccupied).count();
    }
}
