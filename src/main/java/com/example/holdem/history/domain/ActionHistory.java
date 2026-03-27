package com.example.holdem.history.domain;

import jakarta.persistence.Embeddable;

@Embeddable
public class ActionHistory {
    private Long userId;
    private String action;
    private long amount;

    protected ActionHistory() {
    }

    public ActionHistory(Long userId, String action, long amount) {
        this.userId = userId;
        this.action = action;
        this.amount = amount;
    }
}
