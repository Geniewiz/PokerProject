package com.example.holdem.chip.application;

import com.example.holdem.chip.domain.ChipTransaction;
import com.example.holdem.chip.domain.TransactionType;
import com.example.holdem.chip.infrastructure.ChipTransactionRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class ChipLedgerService {
    private final ChipTransactionRepository chipTransactionRepository;

    public ChipLedgerService(ChipTransactionRepository chipTransactionRepository) {
        this.chipTransactionRepository = chipTransactionRepository;
    }

    public void record(Long userId, long amount, TransactionType type) {
        chipTransactionRepository.save(new ChipTransaction(userId, amount, type, Instant.now()));
    }
}
