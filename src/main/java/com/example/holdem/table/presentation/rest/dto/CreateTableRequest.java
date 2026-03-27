package com.example.holdem.table.presentation.rest.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;

public record CreateTableRequest(
        @NotBlank String name,
        @Min(2) @Max(11) int maxPlayers,
        @Min(1) long smallBlind,
        @Min(2) long bigBlind
) {
}
