package com.example.holdem.history.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
public class HandHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String handId;
    private String tableId;
    private Instant playedAt;

    @ElementCollection
    @CollectionTable(name = "hand_history_actions")
    private List<ActionHistory> actions = new ArrayList<>();

    @Embedded
    private ShowdownResult showdownResult;

    protected HandHistory() {
    }

    public HandHistory(String handId, String tableId, Instant playedAt, List<ActionHistory> actions, ShowdownResult showdownResult) {
        this.handId = handId;
        this.tableId = tableId;
        this.playedAt = playedAt;
        this.actions = actions;
        this.showdownResult = showdownResult;
    }
}
