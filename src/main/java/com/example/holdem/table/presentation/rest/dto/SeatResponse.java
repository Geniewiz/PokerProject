package com.example.holdem.table.presentation.rest.dto;

public record SeatResponse(
        int seatNo,
        boolean occupied,
        TablePlayerResponse player
) {
}
