package com.example.holdem.table.presentation.rest.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SitTableRequest(
        @NotNull Long userId,
        @Min(1) int seatNo,
        @Min(1) long buyInAmount
) {
}
