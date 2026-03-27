package com.example.holdem.game.application;

import com.example.holdem.common.error.BusinessException;
import com.example.holdem.common.error.ErrorCode;
import com.example.holdem.game.bot.BotDecision;
import com.example.holdem.game.bot.BotDecisionService;
import com.example.holdem.game.domain.CommunityCards;
import com.example.holdem.game.domain.Hand;
import com.example.holdem.game.domain.HandAction;
import com.example.holdem.game.domain.HandPhase;
import com.example.holdem.game.domain.PlayerActionType;
import com.example.holdem.game.domain.PlayerTurn;
import com.example.holdem.game.domain.Pot;
import com.example.holdem.game.domain.SidePot;
import com.example.holdem.game.domain.TablePlayerSnapshot;
import com.example.holdem.game.engine.ActionValidator;
import com.example.holdem.game.engine.HandStateMachine;
import com.example.holdem.game.engine.PotCalculator;
import com.example.holdem.game.engine.ShowdownResolver;
import com.example.holdem.history.application.HandHistoryService;
import com.example.holdem.history.domain.ActionHistory;
import com.example.holdem.history.domain.ShowdownResult;
import com.example.holdem.table.application.TableCommandService;
import com.example.holdem.table.application.TableEventPublisher;
import com.example.holdem.table.application.TableQueryService;
import com.example.holdem.table.presentation.ws.inbound.ActionCommand;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class ApplyActionUseCase {
    private static final Logger log = LoggerFactory.getLogger(ApplyActionUseCase.class);

    private final GameStateRepository gameStateRepository;
    private final GameEventPublisher gameEventPublisher;
    private final TableQueryService tableQueryService;
    private final TableCommandService tableCommandService;
    private final TableEventPublisher tableEventPublisher;
    private final HandHistoryService handHistoryService;
    private final BotDecisionService botDecisionService;
    private final ActionValidator actionValidator = new ActionValidator();
    private final PotCalculator potCalculator = new PotCalculator();
    private final HandStateMachine handStateMachine = new HandStateMachine();
    private final ShowdownResolver showdownResolver = new ShowdownResolver();
    private final Counter actionAcceptedCounter;
    private final Counter actionRejectedCounter;
    private final Timer actionLatencyTimer;
    private final ApplicationEventPublisher applicationEventPublisher;

    public ApplyActionUseCase(
            GameStateRepository gameStateRepository,
            GameEventPublisher gameEventPublisher,
            TableQueryService tableQueryService,
            TableCommandService tableCommandService,
            TableEventPublisher tableEventPublisher,
            HandHistoryService handHistoryService,
            BotDecisionService botDecisionService,
            MeterRegistry meterRegistry,
            ApplicationEventPublisher applicationEventPublisher
    ) {
        this.gameStateRepository = gameStateRepository;
        this.gameEventPublisher = gameEventPublisher;
        this.tableQueryService = tableQueryService;
        this.tableCommandService = tableCommandService;
        this.tableEventPublisher = tableEventPublisher;
        this.handHistoryService = handHistoryService;
        this.botDecisionService = botDecisionService;
        this.actionAcceptedCounter = meterRegistry.counter("holdem.game.action.accepted");
        this.actionRejectedCounter = meterRegistry.counter("holdem.game.action.rejected");
        this.actionLatencyTimer = meterRegistry.timer("holdem.game.action.latency");
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public Hand apply(ActionCommand command) {
        Timer.Sample sample = Timer.start();
        try {
            Hand hand = gameStateRepository.getRequired(command.tableId());
            PlayerActionType actionType = PlayerActionType.from(command.action());
            if (!actionValidator.canAct(hand, command.userId(), actionType, command.amount())) {
                actionRejectedCounter.increment();
                throw new BusinessException(ErrorCode.INVALID_ACTION);
            }

            Map<Long, Long> updatedCommitted = new HashMap<>(hand.committedChips());
            Map<Long, Long> updatedStreetCommitted = new HashMap<>(hand.streetCommittedChips());
            Map<Long, Long> updatedStacks = new HashMap<>(hand.stackByPlayer());
            Set<Long> updatedFolded = new HashSet<>(hand.foldedPlayerIds());
            Set<Long> updatedAllIn = new HashSet<>(hand.allInPlayerIds());
            List<HandAction> updatedActions = new ArrayList<>(hand.actions());
            List<Long> updatedPending = new ArrayList<>(hand.pendingPlayerIds());

            updatedActions.add(new HandAction(command.userId(), hand.phase(), actionType, command.amount()));

            if (actionType == PlayerActionType.FOLD) {
                updatedFolded.add(command.userId());
            } else {
                subtractStack(updatedStacks, command.userId(), command.amount());
                updatedCommitted.merge(command.userId(), command.amount(), Long::sum);
                updatedStreetCommitted.merge(command.userId(), command.amount(), Long::sum);
                if (updatedStacks.getOrDefault(command.userId(), 0L) == 0L) {
                    updatedAllIn.add(command.userId());
                }
            }
            Pot updatedPot = potCalculator.calculate(updatedCommitted, updatedFolded);
            long updatedCurrentBet = hand.currentBet();

            updatedPending.remove(command.userId());
            if (actionType == PlayerActionType.RAISE) {
                updatedCurrentBet = updatedStreetCommitted.get(command.userId());
                updatedPending = rebuildPendingAfterRaise(hand.turnOrder(), updatedFolded, updatedAllIn, command.userId());
            }

            if (activePlayers(hand.turnOrder(), updatedFolded).size() == 1) {
                Hand finished = finishHand(hand, updatedCommitted, updatedStreetCommitted, updatedStacks, updatedFolded, updatedAllIn, updatedActions, updatedPot, null);
                actionAcceptedCounter.increment();
                return finished;
            }

            if (updatedPending.isEmpty()) {
                Hand progressed = progressStreet(hand, updatedCommitted, updatedStacks, updatedFolded, updatedAllIn, updatedActions, updatedPot);
                actionAcceptedCounter.increment();
                return progressBotTurns(progressed);
            }

            PlayerTurn nextTurn = buildTurn(updatedPending.get(0), updatedStreetCommitted, updatedCurrentBet);
            Map<Long, TablePlayerSnapshot> updatedPlayers = refreshPlayers(hand.players(), updatedStacks);
            Hand updatedHand = new Hand(
                    hand.handId(),
                    hand.tableId(),
                    hand.phase(),
                    hand.communityCards(),
                    updatedPot,
                    nextTurn,
                    Map.copyOf(updatedCommitted),
                    Map.copyOf(updatedStreetCommitted),
                    Map.copyOf(updatedStacks),
                    hand.holeCardsByPlayer(),
                    updatedPlayers,
                    hand.turnOrder(),
                    List.copyOf(updatedPending),
                    Set.copyOf(updatedFolded),
                    Set.copyOf(updatedAllIn),
                    List.copyOf(updatedActions),
                    updatedCurrentBet,
                    hand.bigBlind(),
                    hand.dealerUserId(),
                    null
            );

            tableCommandService.updateChipStacks(hand.tableId(), updatedStacks);
            gameStateRepository.save(updatedHand);
            gameEventPublisher.publishActionResult(updatedHand, command.userId(), command.action(), "ACCEPTED");
            tableEventPublisher.publishSnapshot(tableQueryService.findById(command.tableId()));
            actionAcceptedCounter.increment();
            return progressBotTurns(updatedHand);
        } finally {
            sample.stop(actionLatencyTimer);
        }
    }

    public Hand progressBotTurns(Hand current) {
        return playBotsUntilHumanTurn(current);
    }

    private Hand progressStreet(
            Hand hand,
            Map<Long, Long> updatedCommitted,
            Map<Long, Long> updatedStacks,
            Set<Long> updatedFolded,
            Set<Long> updatedAllIn,
            List<HandAction> updatedActions,
            Pot updatedPot
    ) {
        HandPhase nextPhase = handStateMachine.next(hand.phase());
        if (nextPhase == HandPhase.SHOWDOWN || nextPhase == HandPhase.FINISHED) {
            return finishHand(hand, updatedCommitted, resetStreetCommitted(hand.turnOrder()), updatedStacks, updatedFolded, updatedAllIn, updatedActions, updatedPot, null);
        }

        List<Long> activePlayers = activePlayers(hand.turnOrder(), updatedFolded);
        List<Long> actionPlayers = actionEligiblePlayers(hand.turnOrder(), updatedFolded, updatedAllIn);
        Long firstActor = actionPlayers.isEmpty() ? null : findFirstPostFlopActor(actionPlayers, hand.turnOrder(), hand.dealerUserId());
        List<Long> pending = firstActor == null ? List.of() : rotate(actionPlayers, firstActor);
        CommunityCards communityCards = switch (nextPhase) {
            case FLOP -> new CommunityCards(hand.communityCards().boardCards(), 3);
            case TURN -> new CommunityCards(hand.communityCards().boardCards(), 4);
            case RIVER -> new CommunityCards(hand.communityCards().boardCards(), 5);
            default -> hand.communityCards();
        };

        Map<Long, TablePlayerSnapshot> updatedPlayers = refreshPlayers(hand.players(), updatedStacks);
        Hand updatedHand = new Hand(
                hand.handId(),
                hand.tableId(),
                nextPhase,
                communityCards,
                updatedPot,
                firstActor == null ? null : buildTurn(firstActor, resetStreetCommitted(hand.turnOrder()), 0),
                Map.copyOf(updatedCommitted),
                resetStreetCommitted(hand.turnOrder()),
                Map.copyOf(updatedStacks),
                hand.holeCardsByPlayer(),
                updatedPlayers,
                hand.turnOrder(),
                pending,
                Set.copyOf(updatedFolded),
                Set.copyOf(updatedAllIn),
                List.copyOf(updatedActions),
                0,
                hand.bigBlind(),
                hand.dealerUserId(),
                null
        );

        if (pending.isEmpty() && activePlayers.size() > 1) {
            return progressStreet(updatedHand, updatedCommitted, updatedStacks, updatedFolded, updatedAllIn, updatedActions, updatedPot);
        }

        gameStateRepository.save(updatedHand);
        tableCommandService.updateChipStacks(hand.tableId(), updatedStacks);
        gameEventPublisher.publishActionResult(updatedHand, hand.playerTurn() == null ? null : hand.playerTurn().userId(), "STREET_ADVANCED", "ACCEPTED");
        tableEventPublisher.publishSnapshot(tableQueryService.findById(hand.tableId()));
        return updatedHand;
    }

    private Hand finishHand(
            Hand hand,
            Map<Long, Long> updatedCommitted,
            Map<Long, Long> updatedStreetCommitted,
            Map<Long, Long> updatedStacks,
            Set<Long> updatedFolded,
            Set<Long> updatedAllIn,
            List<HandAction> updatedActions,
            Pot updatedPot,
            Long forceWinnerUserId
    ) {
        Map<Long, Long> payoutByPlayer = distributePot(hand, updatedPot, updatedFolded, forceWinnerUserId);
        payoutByPlayer.forEach((userId, payout) -> updatedStacks.merge(userId, payout, Long::sum));

        Map<Long, TablePlayerSnapshot> updatedPlayers = refreshPlayers(hand.players(), updatedStacks);
        Long winnerUserId = payoutByPlayer.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        Hand finishedHand = new Hand(
                hand.handId(),
                hand.tableId(),
                HandPhase.FINISHED,
                new CommunityCards(hand.communityCards().boardCards(), hand.communityCards().boardCards().size()),
                updatedPot,
                null,
                Map.copyOf(updatedCommitted),
                Map.copyOf(updatedStreetCommitted),
                Map.copyOf(updatedStacks),
                hand.holeCardsByPlayer(),
                updatedPlayers,
                hand.turnOrder(),
                List.of(),
                Set.copyOf(updatedFolded),
                Set.copyOf(updatedAllIn),
                List.copyOf(updatedActions),
                hand.currentBet(),
                hand.bigBlind(),
                hand.dealerUserId(),
                winnerUserId
        );

        gameStateRepository.save(finishedHand);
        tableCommandService.updateChipStacks(hand.tableId(), updatedStacks);
        handHistoryService.save(
                hand.handId(),
                hand.tableId(),
                updatedActions.stream().map(action -> new ActionHistory(action.userId(), action.actionType().name(), action.amount())).toList(),
                new ShowdownResult(winnerUserId, forceWinnerUserId == null ? "SHOWDOWN" : "LAST_PLAYER_STANDING", potCalculator.total(updatedPot))
        );
        gameEventPublisher.publishActionResult(finishedHand, winnerUserId, "HAND_FINISHED", "ACCEPTED");
        tableEventPublisher.publishSnapshot(tableQueryService.findById(hand.tableId()));
        applicationEventPublisher.publishEvent(new HandFinishedEvent(hand.tableId(), hand.handId()));
        botDecisionService.clear(hand.handId());
        log.info("Hand finished tableId={} handId={} payouts={}", hand.tableId(), hand.handId(), payoutByPlayer);
        return finishedHand;
    }

    private Map<Long, Long> distributePot(Hand hand, Pot pot, Set<Long> foldedPlayerIds, Long forceWinnerUserId) {
        Map<Long, Long> payoutByPlayer = new LinkedHashMap<>();
        if (forceWinnerUserId != null) {
            payoutByPlayer.put(forceWinnerUserId, potCalculator.total(pot));
            return payoutByPlayer;
        }

        List<SidePot> allPots = new ArrayList<>();
        if (pot.mainPot() > 0) {
            Set<Long> mainEligible = hand.turnOrder().stream()
                    .filter(userId -> !foldedPlayerIds.contains(userId))
                    .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
            allPots.add(new SidePot(pot.mainPot(), Set.copyOf(mainEligible)));
        }
        allPots.addAll(pot.sidePots());

        for (SidePot sidePot : allPots) {
            List<Long> winners = showdownResolver.resolveWinnersForEligiblePlayers(
                    hand.holeCardsByPlayer(),
                    hand.communityCards().boardCards(),
                    sidePot.eligiblePlayerIds()
            );
            if (winners.isEmpty()) {
                continue;
            }
            long split = sidePot.amount() / winners.size();
            long remainder = sidePot.amount() % winners.size();
            for (Long winner : winners) {
                payoutByPlayer.merge(winner, split, Long::sum);
            }
            for (int i = 0; i < remainder; i++) {
                payoutByPlayer.merge(winners.get(i), 1L, Long::sum);
            }
        }
        return payoutByPlayer;
    }

    private Map<Long, Long> resetStreetCommitted(List<Long> turnOrder) {
        Map<Long, Long> reset = new HashMap<>();
        for (Long userId : turnOrder) {
            reset.put(userId, 0L);
        }
        return Map.copyOf(reset);
    }

    private List<Long> rebuildPendingAfterRaise(List<Long> turnOrder, Set<Long> foldedPlayerIds, Set<Long> allInPlayerIds, Long raiserUserId) {
        List<Long> pending = new ArrayList<>();
        int raiserIndex = turnOrder.indexOf(raiserUserId);
        if (raiserIndex < 0) {
            return List.of();
        }
        for (int i = 1; i < turnOrder.size(); i++) {
            Long candidate = turnOrder.get((raiserIndex + i) % turnOrder.size());
            if (foldedPlayerIds.contains(candidate) || allInPlayerIds.contains(candidate)) {
                continue;
            }
            pending.add(candidate);
        }
        return List.copyOf(pending);
    }

    private List<Long> activePlayers(List<Long> turnOrder, Set<Long> foldedPlayerIds) {
        return turnOrder.stream().filter(userId -> !foldedPlayerIds.contains(userId)).toList();
    }

    private List<Long> actionEligiblePlayers(List<Long> turnOrder, Set<Long> foldedPlayerIds, Set<Long> allInPlayerIds) {
        return turnOrder.stream()
                .filter(userId -> !foldedPlayerIds.contains(userId))
                .filter(userId -> !allInPlayerIds.contains(userId))
                .toList();
    }

    private Long findFirstPostFlopActor(List<Long> activePlayers, List<Long> turnOrder, Long dealerUserId) {
        int dealerIndex = turnOrder.indexOf(dealerUserId);
        for (int i = 1; i <= turnOrder.size(); i++) {
            Long candidate = turnOrder.get((dealerIndex + i) % turnOrder.size());
            if (activePlayers.contains(candidate)) {
                return candidate;
            }
        }
        throw new BusinessException(ErrorCode.INVALID_ACTION);
    }

    private List<Long> rotate(List<Long> players, Long firstActor) {
        int startIndex = players.indexOf(firstActor);
        List<Long> rotated = new ArrayList<>();
        for (int i = 0; i < players.size(); i++) {
            rotated.add(players.get((startIndex + i) % players.size()));
        }
        return List.copyOf(rotated);
    }

    private PlayerTurn buildTurn(Long userId, Map<Long, Long> streetCommitted, long currentBet) {
        long alreadyCommitted = streetCommitted.getOrDefault(userId, 0L);
        long callAmount = Math.max(0, currentBet - alreadyCommitted);
        long minimumRaiseToAmount = callAmount + Math.max(currentBet, 1);
        return new PlayerTurn(userId, callAmount, minimumRaiseToAmount);
    }

    private void subtractStack(Map<Long, Long> stackByPlayer, Long userId, long amount) {
        long stack = stackByPlayer.getOrDefault(userId, 0L);
        if (stack < amount) {
            throw new BusinessException(ErrorCode.CHIP_STACK_NOT_ENOUGH);
        }
        stackByPlayer.put(userId, stack - amount);
    }

    private Map<Long, TablePlayerSnapshot> refreshPlayers(Map<Long, TablePlayerSnapshot> players, Map<Long, Long> stackByPlayer) {
        Map<Long, TablePlayerSnapshot> refreshed = new HashMap<>();
        for (Map.Entry<Long, TablePlayerSnapshot> entry : players.entrySet()) {
            TablePlayerSnapshot snapshot = entry.getValue();
            refreshed.put(
                    entry.getKey(),
                    new TablePlayerSnapshot(
                            snapshot.userId(),
                            snapshot.seatNo(),
                            snapshot.position(),
                            stackByPlayer.getOrDefault(snapshot.userId(), snapshot.stack())
                    )
            );
        }
        return Map.copyOf(refreshed);
    }

    private Hand playBotsUntilHumanTurn(Hand current) {
        Hand cursor = current;
        int guard = 0;
        while (!cursor.isFinished()
                && cursor.playerTurn() != null
                && botDecisionService.isBot(cursor.playerTurn().userId())
                && guard < 32) {
            Long botUserId = cursor.playerTurn().userId();
            pauseForBotThinking(cursor.handId(), botUserId);
            BotDecision decision = botDecisionService.decide(cursor, botUserId);
            if (!actionValidator.canAct(cursor, botUserId, decision.actionType(), decision.amount())) {
                decision = fallbackDecision(cursor, botUserId);
            }
            cursor = applyBotAction(cursor, botUserId, decision);
            guard++;
        }
        if (guard >= 32) {
            log.warn("Bot action guard reached tableId={} handId={}", cursor.tableId(), cursor.handId());
        }
        return cursor;
    }

    private void pauseForBotThinking(String handId, Long botUserId) {
        long delay = Math.max(0, botDecisionService.thinkDelayMillis(handId, botUserId));
        if (delay == 0) {
            return;
        }
        try {
            Thread.sleep(delay);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            log.debug("Bot think delay interrupted");
        }
    }

    private Hand applyBotAction(Hand hand, Long botUserId, BotDecision decision) {
        Map<Long, Long> updatedCommitted = new HashMap<>(hand.committedChips());
        Map<Long, Long> updatedStreetCommitted = new HashMap<>(hand.streetCommittedChips());
        Map<Long, Long> updatedStacks = new HashMap<>(hand.stackByPlayer());
        Set<Long> updatedFolded = new HashSet<>(hand.foldedPlayerIds());
        Set<Long> updatedAllIn = new HashSet<>(hand.allInPlayerIds());
        List<HandAction> updatedActions = new ArrayList<>(hand.actions());
        List<Long> updatedPending = new ArrayList<>(hand.pendingPlayerIds());

        updatedActions.add(new HandAction(botUserId, hand.phase(), decision.actionType(), decision.amount()));
        if (decision.actionType() == PlayerActionType.FOLD) {
            updatedFolded.add(botUserId);
        } else {
            subtractStack(updatedStacks, botUserId, decision.amount());
            updatedCommitted.merge(botUserId, decision.amount(), Long::sum);
            updatedStreetCommitted.merge(botUserId, decision.amount(), Long::sum);
            if (updatedStacks.getOrDefault(botUserId, 0L) == 0L) {
                updatedAllIn.add(botUserId);
            }
        }
        Pot updatedPot = potCalculator.calculate(updatedCommitted, updatedFolded);
        long updatedCurrentBet = hand.currentBet();

        updatedPending.remove(botUserId);
        if (decision.actionType() == PlayerActionType.RAISE) {
            updatedCurrentBet = updatedStreetCommitted.get(botUserId);
            updatedPending = rebuildPendingAfterRaise(hand.turnOrder(), updatedFolded, updatedAllIn, botUserId);
        }

        if (activePlayers(hand.turnOrder(), updatedFolded).size() == 1) {
            return finishHand(hand, updatedCommitted, updatedStreetCommitted, updatedStacks, updatedFolded, updatedAllIn, updatedActions, updatedPot, null);
        }

        if (updatedPending.isEmpty()) {
            return progressStreet(hand, updatedCommitted, updatedStacks, updatedFolded, updatedAllIn, updatedActions, updatedPot);
        }

        PlayerTurn nextTurn = buildTurn(updatedPending.get(0), updatedStreetCommitted, updatedCurrentBet);
        Map<Long, TablePlayerSnapshot> updatedPlayers = refreshPlayers(hand.players(), updatedStacks);
        Hand updatedHand = new Hand(
                hand.handId(),
                hand.tableId(),
                hand.phase(),
                hand.communityCards(),
                updatedPot,
                nextTurn,
                Map.copyOf(updatedCommitted),
                Map.copyOf(updatedStreetCommitted),
                Map.copyOf(updatedStacks),
                hand.holeCardsByPlayer(),
                updatedPlayers,
                hand.turnOrder(),
                List.copyOf(updatedPending),
                Set.copyOf(updatedFolded),
                Set.copyOf(updatedAllIn),
                List.copyOf(updatedActions),
                updatedCurrentBet,
                hand.bigBlind(),
                hand.dealerUserId(),
                null
        );
        gameStateRepository.save(updatedHand);
        tableCommandService.updateChipStacks(hand.tableId(), updatedStacks);
        gameEventPublisher.publishActionResult(updatedHand, botUserId, decision.actionType().name(), "ACCEPTED");
        tableEventPublisher.publishSnapshot(tableQueryService.findById(hand.tableId()));
        actionAcceptedCounter.increment();
        return updatedHand;
    }

    private BotDecision fallbackDecision(Hand hand, Long botUserId) {
        long stack = hand.stackByPlayer().getOrDefault(botUserId, 0L);
        long callAmount = hand.playerTurn() == null ? 0L : hand.playerTurn().callAmount();
        if (callAmount == 0) {
            return new BotDecision(PlayerActionType.CHECK, 0);
        }
        if (stack > 0) {
            return new BotDecision(PlayerActionType.CALL, Math.min(callAmount, stack));
        }
        return new BotDecision(PlayerActionType.FOLD, 0);
    }
}
