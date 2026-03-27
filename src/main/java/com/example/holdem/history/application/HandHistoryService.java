package com.example.holdem.history.application;

import com.example.holdem.history.domain.ActionHistory;
import com.example.holdem.history.domain.HandHistory;
import com.example.holdem.history.domain.ShowdownResult;
import com.example.holdem.history.infrastructure.HandHistoryJpaRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class HandHistoryService {
    private final HandHistoryJpaRepository handHistoryJpaRepository;

    public HandHistoryService(HandHistoryJpaRepository handHistoryJpaRepository) {
        this.handHistoryJpaRepository = handHistoryJpaRepository;
    }

    public HandHistory save(String handId, String tableId, List<ActionHistory> actions, ShowdownResult showdownResult) {
        return handHistoryJpaRepository.save(new HandHistory(handId, tableId, Instant.now(), actions, showdownResult));
    }
}
