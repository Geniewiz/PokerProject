package com.example.holdem.game.bot;

import com.example.holdem.game.domain.Hand;
import com.example.holdem.game.domain.PlayerActionType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class BotDecisionService {
    private final Random random = new Random();
    private final ConcurrentHashMap<String, Map<Long, BotStrategyType>> handStrategies = new ConcurrentHashMap<>();

    public void assignStrategies(String handId, Set<Long> botUserIds) {
        Map<Long, BotStrategyType> strategies = new HashMap<>();
        for (Long botUserId : botUserIds) {
            strategies.put(botUserId, randomStrategy());
        }
        handStrategies.put(handId, Map.copyOf(strategies));
    }

    public boolean isBot(Long userId) {
        return BotIdentity.isBot(userId);
    }

    public BotDecision decide(Hand hand, Long botUserId) {
        BotStrategyType strategy = strategyOf(hand.handId(), botUserId);
        long stack = hand.stackByPlayer().getOrDefault(botUserId, 0L);
        long callAmount = hand.playerTurn() == null ? 0L : hand.playerTurn().callAmount();
        long minRaise = hand.playerTurn() == null ? 0L : hand.playerTurn().minimumRaiseToAmount();

        return switch (strategy) {
            case TIGHT_AGGRESSIVE -> tightAggressive(callAmount, minRaise, stack, hand.bigBlind());
            case LOOSE_AGGRESSIVE -> looseAggressive(callAmount, minRaise, stack, hand.bigBlind());
            case PASSIVE_TRAPPY -> passiveTrappy(callAmount, minRaise, stack, hand.bigBlind());
        };
    }

    public void clear(String handId) {
        handStrategies.remove(handId);
    }

    public long thinkDelayMillis(String handId, Long botUserId) {
        BotStrategyType strategy = strategyOf(handId, botUserId);
        return switch (strategy) {
            case TIGHT_AGGRESSIVE -> randomBetween(1000, 2100);
            case LOOSE_AGGRESSIVE -> randomBetween(450, 1200);
            case PASSIVE_TRAPPY -> randomBetween(1400, 2800);
        };
    }

    private BotDecision tightAggressive(long callAmount, long minRaise, long stack, long bigBlind) {
        if (callAmount == 0) {
            if (stack >= minRaise && chance(0.18)) {
                return new BotDecision(PlayerActionType.RAISE, pickRaiseAmount(minRaise, stack, bigBlind, 2.0));
            }
            return new BotDecision(PlayerActionType.CHECK, 0);
        }
        if (callAmount > stack * 0.30 && chance(0.70)) {
            return new BotDecision(PlayerActionType.FOLD, 0);
        }
        if (stack >= minRaise && chance(0.20)) {
            return new BotDecision(PlayerActionType.RAISE, pickRaiseAmount(minRaise, stack, bigBlind, 2.5));
        }
        return new BotDecision(PlayerActionType.CALL, Math.min(callAmount, stack));
    }

    private BotDecision looseAggressive(long callAmount, long minRaise, long stack, long bigBlind) {
        if (callAmount == 0) {
            if (stack >= minRaise && chance(0.45)) {
                return new BotDecision(PlayerActionType.RAISE, pickRaiseAmount(minRaise, stack, bigBlind, 3.5));
            }
            return new BotDecision(PlayerActionType.CHECK, 0);
        }
        if (stack >= minRaise && chance(0.35)) {
            return new BotDecision(PlayerActionType.RAISE, pickRaiseAmount(minRaise, stack, bigBlind, 4.0));
        }
        if (chance(0.85)) {
            return new BotDecision(PlayerActionType.CALL, Math.min(callAmount, stack));
        }
        return new BotDecision(PlayerActionType.FOLD, 0);
    }

    private BotDecision passiveTrappy(long callAmount, long minRaise, long stack, long bigBlind) {
        if (callAmount == 0) {
            if (stack >= minRaise && chance(0.08)) {
                return new BotDecision(PlayerActionType.RAISE, pickRaiseAmount(minRaise, stack, bigBlind, 2.0));
            }
            return new BotDecision(PlayerActionType.CHECK, 0);
        }
        if (stack >= minRaise && chance(0.12)) {
            return new BotDecision(PlayerActionType.RAISE, pickRaiseAmount(minRaise, stack, bigBlind, 2.2));
        }
        if (chance(0.78)) {
            return new BotDecision(PlayerActionType.CALL, Math.min(callAmount, stack));
        }
        return new BotDecision(PlayerActionType.FOLD, 0);
    }

    private long pickRaiseAmount(long minRaise, long stack, long bigBlind, double multiple) {
        long cap = Math.min(stack, minRaise + Math.round(bigBlind * multiple));
        if (cap <= minRaise) {
            return stack;
        }
        List<Long> candidates = new ArrayList<>();
        candidates.add(minRaise);
        candidates.add((minRaise + cap) / 2);
        candidates.add(cap);
        return candidates.get(random.nextInt(candidates.size()));
    }

    private boolean chance(double probability) {
        return random.nextDouble() <= probability;
    }

    private BotStrategyType randomStrategy() {
        BotStrategyType[] values = BotStrategyType.values();
        return values[random.nextInt(values.length)];
    }

    private BotStrategyType strategyOf(String handId, Long botUserId) {
        BotStrategyType strategy = handStrategies.getOrDefault(handId, Map.of()).get(botUserId);
        if (strategy != null) {
            return strategy;
        }
        return randomStrategy();
    }

    private long randomBetween(long minInclusive, long maxInclusive) {
        if (maxInclusive <= minInclusive) {
            return minInclusive;
        }
        return minInclusive + random.nextInt((int) (maxInclusive - minInclusive + 1));
    }
}
