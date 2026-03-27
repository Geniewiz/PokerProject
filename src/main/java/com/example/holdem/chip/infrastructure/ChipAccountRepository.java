package com.example.holdem.chip.infrastructure;

import com.example.holdem.chip.domain.ChipAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChipAccountRepository extends JpaRepository<ChipAccount, Long> {
    Optional<ChipAccount> findByUserId(Long userId);
}
