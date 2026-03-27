package com.example.holdem.game.presentation;

import com.example.holdem.common.response.ApiResponse;
import com.example.holdem.game.application.GameStateRepository;
import com.example.holdem.game.application.StartHandUseCase;
import com.example.holdem.game.presentation.dto.GameSnapshotMapper;
import com.example.holdem.game.presentation.dto.GameSnapshotResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/games")
public class GameController {
    private final StartHandUseCase startHandUseCase;
    private final GameStateRepository gameStateRepository;
    private final GameSnapshotMapper gameSnapshotMapper;

    public GameController(
            StartHandUseCase startHandUseCase,
            GameStateRepository gameStateRepository,
            GameSnapshotMapper gameSnapshotMapper
    ) {
        this.startHandUseCase = startHandUseCase;
        this.gameStateRepository = gameStateRepository;
        this.gameSnapshotMapper = gameSnapshotMapper;
    }

    @PostMapping("/tables/{tableId}/start")
    public ApiResponse<GameSnapshotResponse> start(
            @PathVariable String tableId,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "false") boolean withBot,
            @RequestParam(defaultValue = "1") int botCount
    ) {
        return ApiResponse.ok(gameSnapshotMapper.from(startHandUseCase.start(tableId, withBot, botCount), userId));
    }

    @GetMapping("/tables/{tableId}")
    public ApiResponse<GameSnapshotResponse> get(@PathVariable String tableId, @RequestParam(required = false) Long userId) {
        return ApiResponse.ok(gameSnapshotMapper.from(gameStateRepository.getRequired(tableId), userId));
    }
}
