package com.example.holdem.game.application;

import com.example.holdem.common.error.BusinessException;
import com.example.holdem.common.error.ErrorCode;
import com.example.holdem.game.bot.BotDecisionService;
import com.example.holdem.game.bot.BotIdentity;
import com.example.holdem.game.domain.CommunityCards;
import com.example.holdem.game.domain.Hand;
import com.example.holdem.game.domain.HandAction;
import com.example.holdem.game.domain.HandPhase;
import com.example.holdem.game.domain.PlayerActionType;
import com.example.holdem.game.domain.PlayerTurn;
import com.example.holdem.game.domain.Pot;
import com.example.holdem.game.domain.TablePlayerSnapshot;
import com.example.holdem.game.engine.Card;
import com.example.holdem.game.engine.Deck;
import com.example.holdem.game.engine.HoleCards;
import com.example.holdem.table.application.TableCommandService;
import com.example.holdem.table.application.TableQueryService;
import com.example.holdem.table.domain.Seat;
import com.example.holdem.table.domain.Table;
import com.example.holdem.table.domain.TablePosition;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class StartHandUseCase {
    private final GameStateRepository gameStateRepository;
    private final TableQueryService tableQueryService;
    private final TableCommandService tableCommandService;
    private final BotDecisionService botDecisionService;
    private final ApplyActionUseCase applyActionUseCase;
    private final GameEventPublisher gameEventPublisher;

    public StartHandUseCase(
            GameStateRepository gameStateRepository,
            TableQueryService tableQueryService,
            TableCommandService tableCommandService,
            BotDecisionService botDecisionService,
            ApplyActionUseCase applyActionUseCase,
            GameEventPublisher gameEventPublisher
    ) {
        this.gameStateRepository = gameStateRepository;
        this.tableQueryService = tableQueryService;
        this.tableCommandService = tableCommandService;
        this.botDecisionService = botDecisionService;
        this.applyActionUseCase = applyActionUseCase;
        this.gameEventPublisher = gameEventPublisher;
    }

    public Hand start(String tableId) {
        return start(tableId, false, 1);
    }

    public Hand start(String tableId, boolean withBot) {
        return start(tableId, withBot, 1);
    }

    public Hand start(String tableId, boolean withBot, int botCount) {
        Table table = tableQueryService.findById(tableId);
        List<Seat> occupiedSeats = table.seats().stream()
                .filter(Seat::isOccupied)
                .filter(seat -> seat.player().ready())
                .filter(seat -> seat.player().chipStack() > 0)
                .sorted(java.util.Comparator.comparingInt(Seat::seatNo))
                .toList();
        if (occupiedSeats.size() < 2) {
            long rebuyAmount = Math.max(table.blindPolicy().bigBlind() * 50, 1000);
            table = tableCommandService.rebuyBustedBots(tableId, rebuyAmount);
            occupiedSeats = table.seats().stream()
                    .filter(Seat::isOccupied)
                    .filter(seat -> seat.player().ready())
                    .filter(seat -> seat.player().chipStack() > 0)
                    .sorted(java.util.Comparator.comparingInt(Seat::seatNo))
                    .toList();
        }
        if (withBot && occupiedSeats.size() >= 1 && botCount > 0) {
            table = tableCommandService.ensureBotOpponents(tableId, botCount, occupiedSeats.get(0).player().chipStack());
            occupiedSeats = table.seats().stream()
                    .filter(Seat::isOccupied)
                    .filter(seat -> seat.player().ready())
                    .filter(seat -> seat.player().chipStack() > 0)
                    .sorted(java.util.Comparator.comparingInt(Seat::seatNo))
                    .toList();
        }
        if (occupiedSeats.size() < 2) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_PLAYERS);
        }

        int dealerSeatNo = resolveNextDealerSeatNo(table, occupiedSeats);
        List<Seat> orderedFromDealer = rotateSeatsFromDealer(occupiedSeats, dealerSeatNo);
        List<Long> turnOrder = orderedFromDealer.stream().map(seat -> seat.player().userId()).toList();
        Long dealerUserId = orderedFromDealer.get(0).player().userId();
        Long smallBlindUserId = orderedFromDealer.size() == 2
                ? orderedFromDealer.get(0).player().userId()
                : orderedFromDealer.get(1).player().userId();
        Long bigBlindUserId = orderedFromDealer.size() == 2
                ? orderedFromDealer.get(1).player().userId()
                : orderedFromDealer.get(2).player().userId();
        int firstActorIndex = turnOrder.size() == 2 || turnOrder.size() == 3 ? 0 : 3;
        List<Long> pendingPlayerIds = buildPending(turnOrder, firstActorIndex);

        Map<Long, Long> stackByPlayer = new LinkedHashMap<>();
        for (Seat occupiedSeat : orderedFromDealer) {
            stackByPlayer.put(occupiedSeat.player().userId(), occupiedSeat.player().chipStack());
        }

        postBlind(stackByPlayer, smallBlindUserId, table.blindPolicy().smallBlind());
        postBlind(stackByPlayer, bigBlindUserId, table.blindPolicy().bigBlind());
        Set<Long> allInPlayerIds = new HashSet<>();
        if (stackByPlayer.getOrDefault(smallBlindUserId, 0L) == 0L) {
            allInPlayerIds.add(smallBlindUserId);
        }
        if (stackByPlayer.getOrDefault(bigBlindUserId, 0L) == 0L) {
            allInPlayerIds.add(bigBlindUserId);
        }

        Map<Long, Long> committedChips = new LinkedHashMap<>();
        committedChips.put(smallBlindUserId, table.blindPolicy().smallBlind());
        committedChips.put(bigBlindUserId, table.blindPolicy().bigBlind());

        Map<Long, Long> streetCommitted = new LinkedHashMap<>();
        for (Long userId : turnOrder) {
            streetCommitted.put(userId, 0L);
        }
        streetCommitted.put(smallBlindUserId, table.blindPolicy().smallBlind());
        streetCommitted.put(bigBlindUserId, table.blindPolicy().bigBlind());

        Deck deck = new Deck();
        Map<Long, HoleCards> holeCardsByPlayer = new LinkedHashMap<>();
        for (Long userId : turnOrder) {
            holeCardsByPlayer.put(userId, new HoleCards(deck.draw(), deck.draw()));
        }
        List<Card> boardCards = List.of(deck.draw(), deck.draw(), deck.draw(), deck.draw(), deck.draw());
        Map<Long, TablePlayerSnapshot> players = buildPlayerSnapshots(orderedFromDealer, stackByPlayer);

        tableCommandService.updateDealerButton(tableId, dealerSeatNo);
        tableCommandService.updateChipStacks(tableId, stackByPlayer);
        Hand hand = new Hand(
                "hand-" + UUID.randomUUID(),
                tableId,
                HandPhase.PREFLOP,
                new CommunityCards(boardCards, 0),
                new Pot(table.blindPolicy().smallBlind() + table.blindPolicy().bigBlind(), List.of()),
                buildTurn(pendingPlayerIds.get(0), streetCommitted, table.blindPolicy().bigBlind()),
                Map.copyOf(committedChips),
                Map.copyOf(streetCommitted),
                Map.copyOf(stackByPlayer),
                Map.copyOf(holeCardsByPlayer),
                Map.copyOf(players),
                turnOrder,
                pendingPlayerIds.stream().filter(userId -> !allInPlayerIds.contains(userId)).toList(),
                Set.of(),
                Set.copyOf(allInPlayerIds),
                List.of(
                        new HandAction(smallBlindUserId, HandPhase.PREFLOP, PlayerActionType.CALL, table.blindPolicy().smallBlind()),
                        new HandAction(bigBlindUserId, HandPhase.PREFLOP, PlayerActionType.CALL, table.blindPolicy().bigBlind())
                ),
                table.blindPolicy().bigBlind(),
                table.blindPolicy().bigBlind(),
                dealerUserId,
                null
        );
        Set<Long> botUserIds = hand.turnOrder().stream().filter(BotIdentity::isBot).collect(java.util.stream.Collectors.toSet());
        if (!botUserIds.isEmpty()) {
            botDecisionService.assignStrategies(hand.handId(), botUserIds);
        }
        gameStateRepository.save(hand);
        gameEventPublisher.publishActionResult(hand, null, "HAND_STARTED", "ACCEPTED");
        return applyActionUseCase.progressBotTurns(hand);
    }

    private int resolveNextDealerSeatNo(Table table, List<Seat> occupiedSeats) {
        if (table.dealerSeatNo() == null) {
            return occupiedSeats.get(0).seatNo();
        }
        for (Seat seat : occupiedSeats) {
            if (seat.seatNo() > table.dealerSeatNo()) {
                return seat.seatNo();
            }
        }
        return occupiedSeats.get(0).seatNo();
    }

    private List<Seat> rotateSeatsFromDealer(List<Seat> occupiedSeats, int dealerSeatNo) {
        int dealerIndex = 0;
        for (int i = 0; i < occupiedSeats.size(); i++) {
            if (occupiedSeats.get(i).seatNo() == dealerSeatNo) {
                dealerIndex = i;
                break;
            }
        }
        List<Seat> rotated = new ArrayList<>();
        for (int i = 0; i < occupiedSeats.size(); i++) {
            rotated.add(occupiedSeats.get((dealerIndex + i) % occupiedSeats.size()));
        }
        return List.copyOf(rotated);
    }

    private Map<Long, TablePlayerSnapshot> buildPlayerSnapshots(List<Seat> orderedFromDealer, Map<Long, Long> stackByPlayer) {
        List<String> positions = TablePosition.labelsForPlayerCount(orderedFromDealer.size());
        Map<Long, TablePlayerSnapshot> players = new LinkedHashMap<>();
        for (int i = 0; i < orderedFromDealer.size(); i++) {
            Seat seat = orderedFromDealer.get(i);
            players.put(
                    seat.player().userId(),
                    new TablePlayerSnapshot(
                            seat.player().userId(),
                            seat.seatNo(),
                            positions.get(i),
                            stackByPlayer.getOrDefault(seat.player().userId(), seat.player().chipStack())
                    )
            );
        }
        return players;
    }

    private void postBlind(Map<Long, Long> stackByPlayer, Long userId, long amount) {
        long stack = stackByPlayer.getOrDefault(userId, 0L);
        long posted = Math.min(stack, amount);
        stackByPlayer.put(userId, stack - posted);
    }

    private List<Long> buildPending(List<Long> turnOrder, int startIndex) {
        java.util.ArrayList<Long> pending = new java.util.ArrayList<>();
        for (int i = 0; i < turnOrder.size(); i++) {
            pending.add(turnOrder.get((startIndex + i) % turnOrder.size()));
        }
        return List.copyOf(pending);
    }

    private PlayerTurn buildTurn(Long userId, Map<Long, Long> streetCommitted, long currentBet) {
        long alreadyCommitted = streetCommitted.getOrDefault(userId, 0L);
        long callAmount = Math.max(0, currentBet - alreadyCommitted);
        long minimumRaiseToAmount = callAmount + currentBet;
        return new PlayerTurn(userId, callAmount, minimumRaiseToAmount);
    }
}
