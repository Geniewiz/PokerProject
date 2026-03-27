package com.example.holdem.game.application;

import com.example.holdem.game.domain.Hand;
import com.example.holdem.game.presentation.dto.GameSnapshotMapper;
import com.example.holdem.table.presentation.ws.outbound.ActionResultMessage;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class GameEventPublisher {
    private final SimpMessagingTemplate messagingTemplate;
    private final GameSnapshotMapper gameSnapshotMapper;

    public GameEventPublisher(SimpMessagingTemplate messagingTemplate, GameSnapshotMapper gameSnapshotMapper) {
        this.messagingTemplate = messagingTemplate;
        this.gameSnapshotMapper = gameSnapshotMapper;
    }

    public void publishActionResult(Hand hand, Long userId, String action, String status) {
        messagingTemplate.convertAndSend(
                "/topic/tables/" + hand.tableId() + "/actions",
                new ActionResultMessage(
                        hand.tableId(),
                        userId,
                        action,
                        status,
                        gameSnapshotMapper.from(hand, null)
                )
        );
    }
}
