package com.example.holdem.chip.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;

@Entity
public class ChipTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private long amount;

    @Enumerated(EnumType.STRING)
    private TransactionType type;

    private Instant createdAt;

    protected ChipTransaction() {
    }

    public ChipTransaction(Long userId, long amount, TransactionType type, Instant createdAt) {
        this.userId = userId;
        this.amount = amount;
        this.type = type;
        this.createdAt = createdAt;
    }
}
