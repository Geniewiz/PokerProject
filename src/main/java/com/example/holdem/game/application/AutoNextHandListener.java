package com.example.holdem.game.application;

import com.example.holdem.common.error.BusinessException;
import com.example.holdem.common.error.ErrorCode;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class AutoNextHandListener {
    private static final Logger log = LoggerFactory.getLogger(AutoNextHandListener.class);

    private final StartHandUseCase startHandUseCase;
    private final GameStateRepository gameStateRepository;
    private final boolean enabled;
    private final long delayMs;

    public AutoNextHandListener(
            StartHandUseCase startHandUseCase,
            GameStateRepository gameStateRepository,
            @Value("${holdem.game.auto-next-hand.enabled:true}") boolean enabled,
            @Value("${holdem.game.auto-next-hand.delay-ms:2200}") long delayMs
    ) {
        this.startHandUseCase = startHandUseCase;
        this.gameStateRepository = gameStateRepository;
        this.enabled = enabled;
        this.delayMs = Math.max(0, delayMs);
    }

    @EventListener
    public void onHandFinished(HandFinishedEvent event) {
        if (!enabled) {
            return;
        }
        CompletableFuture.runAsync(() -> startNextHand(event.tableId()));
    }

    private void startNextHand(String tableId) {
        try {
            if (delayMs > 0) {
                Thread.sleep(delayMs);
            }
            var currentHand = gameStateRepository.findByTableId(tableId).orElse(null);
            if (currentHand != null && !currentHand.isFinished()) {
                return;
            }
            startHandUseCase.start(tableId, false, 0);
            log.info("Auto next hand started tableId={}", tableId);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            log.debug("Auto next hand interrupted tableId={}", tableId);
        } catch (BusinessException businessException) {
            if (businessException.getErrorCode() == ErrorCode.INSUFFICIENT_PLAYERS) {
                log.info("Auto next hand skipped (insufficient players) tableId={}", tableId);
                return;
            }
            log.warn("Auto next hand failed tableId={} code={}", tableId, businessException.getErrorCode(), businessException);
        } catch (Exception exception) {
            log.warn("Auto next hand failed tableId={}", tableId, exception);
        }
    }
}
