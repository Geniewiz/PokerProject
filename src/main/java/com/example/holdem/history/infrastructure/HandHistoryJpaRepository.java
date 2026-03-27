package com.example.holdem.history.infrastructure;

import com.example.holdem.history.domain.HandHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HandHistoryJpaRepository extends JpaRepository<HandHistory, Long> {
}
