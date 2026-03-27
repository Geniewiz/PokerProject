package com.example.holdem.table.presentation.ws.outbound;

import com.example.holdem.table.presentation.rest.dto.TableSnapshotResponse;

public record TableSnapshotMessage(
        TableSnapshotResponse table
) {
}
