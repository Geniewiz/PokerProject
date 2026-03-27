package com.example.holdem.table.domain;

public record Seat(
        int seatNo,
        TablePlayer player
) {
    public boolean isOccupied() {
        return player != null;
    }
}
