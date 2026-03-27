package com.example.holdem.chip.infrastructure;

import com.example.holdem.chip.domain.ChipTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChipTransactionRepository extends JpaRepository<ChipTransaction, Long> {
}
