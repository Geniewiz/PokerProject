package com.example.holdem.lobby.application;

import com.example.holdem.lobby.domain.TableSummary;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class LobbyQueryService {

    public List<TableSummary> getLobbyTables() {
        return List.of(
                new TableSummary("table-1", "Beginner Holdem", 11, 0, 50, 100)
        );
    }
}
