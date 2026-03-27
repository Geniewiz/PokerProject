package com.example.holdem.chip.application;

import com.example.holdem.chip.domain.ChipAccount;
import com.example.holdem.chip.domain.TransactionType;
import com.example.holdem.chip.infrastructure.ChipAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BuyInService {
    private final ChipAccountRepository chipAccountRepository;
    private final ChipLedgerService chipLedgerService;

    public BuyInService(ChipAccountRepository chipAccountRepository, ChipLedgerService chipLedgerService) {
        this.chipAccountRepository = chipAccountRepository;
        this.chipLedgerService = chipLedgerService;
    }

    @Transactional
    public long buyIn(Long userId, long amount) {
        ChipAccount account = chipAccountRepository.findByUserId(userId)
                .orElseGet(() -> chipAccountRepository.save(new ChipAccount(userId, 0)));
        account.debit(amount);
        chipLedgerService.record(userId, amount, TransactionType.BUY_IN);
        return amount;
    }
}
