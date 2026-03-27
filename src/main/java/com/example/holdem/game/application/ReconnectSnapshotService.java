package com.example.holdem.game.application;

import com.example.holdem.game.domain.Hand;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ReconnectSnapshotService {
    private final GameStateRepository gameStateRepository;

    public ReconnectSnapshotService(GameStateRepository gameStateRepository) {
        this.gameStateRepository = gameStateRepository;
    }

    public Optional<Hand> getSnapshot(String tableId) {
        return gameStateRepository.findByTableId(tableId);
    }
}
