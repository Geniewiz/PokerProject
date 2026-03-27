package com.example.holdem.lobby.presentation;

import com.example.holdem.common.response.ApiResponse;
import com.example.holdem.lobby.application.LobbyQueryService;
import com.example.holdem.lobby.domain.TableSummary;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/lobby")
public class LobbyController {
    private final LobbyQueryService lobbyQueryService;

    public LobbyController(LobbyQueryService lobbyQueryService) {
        this.lobbyQueryService = lobbyQueryService;
    }

    @GetMapping("/tables")
    public ApiResponse<List<TableSummary>> getTables() {
        return ApiResponse.ok(lobbyQueryService.getLobbyTables());
    }
}
