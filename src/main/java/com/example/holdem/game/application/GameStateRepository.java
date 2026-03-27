package com.example.holdem.game.application;

import com.example.holdem.common.error.BusinessException;
import com.example.holdem.common.error.ErrorCode;
import com.example.holdem.game.domain.Hand;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public interface GameStateRepository {
    Hand save(Hand hand);
    Optional<Hand> findByTableId(String tableId);

    default Hand getRequired(String tableId) {
        return findByTableId(tableId).orElseThrow(() -> new BusinessException(ErrorCode.HAND_NOT_FOUND));
    }
}
