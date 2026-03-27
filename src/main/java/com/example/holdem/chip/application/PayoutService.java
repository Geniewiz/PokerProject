package com.example.holdem.chip.application;

import com.example.holdem.chip.domain.ChipAccount;
import com.example.holdem.chip.domain.TransactionType;
import com.example.holdem.chip.infrastructure.ChipAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PayoutService {
    private final ChipAccountRepository chipAccountRepository;
    private final ChipLedgerService chipLedgerService;

    public PayoutService(ChipAccountRepository chipAccountRepository, ChipLedgerService chipLedgerService) {
        this.chipAccountRepository = chipAccountRepository;
        this.chipLedgerService = chipLedgerService;
    }

    @Transactional
    public void payout(Long userId, long amount) {
        ChipAccount account = chipAccountRepository.findByUserId(userId)
                .orElseGet(() -> chipAccountRepository.save(new ChipAccount(userId, 0)));
        account.credit(amount);
        chipLedgerService.record(userId, amount, TransactionType.PAYOUT);
    }
}
