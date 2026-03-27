package com.example.holdem.table.presentation.rest.dto;

import com.example.holdem.table.domain.Seat;
import com.example.holdem.table.domain.Table;
import com.example.holdem.table.domain.TablePlayer;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TableSnapshotMapper {

    public TableSnapshotResponse from(Table table) {
        return new TableSnapshotResponse(
                table.tableId(),
                table.name(),
                table.status().name(),
                table.blindPolicy().smallBlind(),
                table.blindPolicy().bigBlind(),
                table.dealerSeatNo(),
                table.seatedPlayerCount(),
                table.seats().stream().map(this::toSeat).toList()
        );
    }

    private SeatResponse toSeat(Seat seat) {
        return new SeatResponse(seat.seatNo(), seat.isOccupied(), toPlayer(seat.player()));
    }

    private TablePlayerResponse toPlayer(TablePlayer player) {
        if (player == null) {
            return null;
        }
        return new TablePlayerResponse(
                player.userId(),
                player.nickname(),
                player.chipStack(),
                player.connected(),
                player.ready()
        );
    }
}
