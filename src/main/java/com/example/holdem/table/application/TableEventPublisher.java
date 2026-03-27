package com.example.holdem.table.application;

import com.example.holdem.table.domain.Table;
import com.example.holdem.table.presentation.rest.dto.TableSnapshotMapper;
import com.example.holdem.table.presentation.ws.outbound.TableSnapshotMessage;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class TableEventPublisher {
    private final SimpMessagingTemplate messagingTemplate;
    private final TableSnapshotMapper tableSnapshotMapper;

    public TableEventPublisher(SimpMessagingTemplate messagingTemplate, TableSnapshotMapper tableSnapshotMapper) {
        this.messagingTemplate = messagingTemplate;
        this.tableSnapshotMapper = tableSnapshotMapper;
    }

    public void publishSnapshot(Table table) {
        messagingTemplate.convertAndSend(
                "/topic/tables/" + table.tableId(),
                new TableSnapshotMessage(tableSnapshotMapper.from(table))
        );
    }
}
