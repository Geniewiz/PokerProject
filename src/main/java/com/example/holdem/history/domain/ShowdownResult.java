package com.example.holdem.history.domain;

import jakarta.persistence.Embeddable;

@Embeddable
public class ShowdownResult {
    private Long winnerUserId;
    private String handRank;
    private long payoutAmount;

    protected ShowdownResult() {
    }

    public ShowdownResult(Long winnerUserId, String handRank, long payoutAmount) {
        this.winnerUserId = winnerUserId;
        this.handRank = handRank;
        this.payoutAmount = payoutAmount;
    }
}
