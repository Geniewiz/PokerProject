package com.example.holdem.table.application;

import com.example.holdem.common.error.BusinessException;
import com.example.holdem.common.error.ErrorCode;
import com.example.holdem.game.bot.BotIdentity;
import com.example.holdem.table.domain.BlindPolicy;
import com.example.holdem.table.domain.Seat;
import com.example.holdem.table.domain.Table;
import com.example.holdem.table.domain.TablePlayer;
import com.example.holdem.table.domain.TableStatus;
import com.example.holdem.table.infrastructure.redis.TableRedisRepository;
import com.example.holdem.table.presentation.rest.dto.CreateTableRequest;
import com.example.holdem.table.presentation.ws.inbound.LeaveCommand;
import com.example.holdem.table.presentation.ws.inbound.SitCommand;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

@Service
public class TableCommandService {
    private static final AtomicLong BOT_SEQUENCE = new AtomicLong(BotIdentity.BOT_USER_ID_BASE);
    private final TableRedisRepository tableRedisRepository;
    private final TableEventPublisher tableEventPublisher;

    public TableCommandService(TableRedisRepository tableRedisRepository, TableEventPublisher tableEventPublisher) {
        this.tableRedisRepository = tableRedisRepository;
        this.tableEventPublisher = tableEventPublisher;
    }

    public Table createTable(CreateTableRequest request) {
        List<Seat> seats = new ArrayList<>();
        for (int i = 1; i <= request.maxPlayers(); i++) {
            seats.add(new Seat(i, null));
        }
        Table table = new Table(
                "table-" + UUID.randomUUID(),
                request.name(),
                TableStatus.WAITING,
                new BlindPolicy(request.smallBlind(), request.bigBlind(), 0),
                seats,
                null
        );
        tableRedisRepository.save(table);
        return table;
    }

    public void sit(SitCommand command) {
        Table table = tableRedisRepository.getRequired(command.tableId());
        if (command.seatNo() < 1 || command.seatNo() > table.seats().size()) {
            throw new BusinessException(ErrorCode.SEAT_NOT_AVAILABLE);
        }
        if (table.seats().get(command.seatNo() - 1).isOccupied()) {
            throw new BusinessException(ErrorCode.SEAT_NOT_AVAILABLE);
        }
        List<Seat> updatedSeats = new ArrayList<>(table.seats());
        updatedSeats.set(
                command.seatNo() - 1,
                new Seat(command.seatNo(), new TablePlayer(command.userId(), "player-" + command.userId(), command.buyInAmount(), true, true))
        );
        Table updatedTable = new Table(table.tableId(), table.name(), table.status(), table.blindPolicy(), updatedSeats, table.dealerSeatNo());
        tableRedisRepository.save(updatedTable);
        tableEventPublisher.publishSnapshot(updatedTable);
    }

    public void leave(LeaveCommand command) {
        Table table = tableRedisRepository.getRequired(command.tableId());
        List<Seat> updatedSeats = table.seats().stream()
                .map(seat -> seat.player() != null && seat.player().userId().equals(command.userId())
                        ? new Seat(seat.seatNo(), null)
                        : seat)
                .toList();
        Integer nextDealerSeatNo = table.dealerSeatNo();
        final Integer dealerSeatNo = nextDealerSeatNo;
        if (dealerSeatNo != null && updatedSeats.stream().noneMatch(seat -> seat.seatNo() == dealerSeatNo && seat.isOccupied())) {
            nextDealerSeatNo = null;
        }
        Table updatedTable = new Table(table.tableId(), table.name(), table.status(), table.blindPolicy(), updatedSeats, nextDealerSeatNo);
        tableRedisRepository.save(updatedTable);
        tableEventPublisher.publishSnapshot(updatedTable);
    }

    public Table updateChipStacks(String tableId, Map<Long, Long> stackByPlayer) {
        Table table = tableRedisRepository.getRequired(tableId);
        List<Seat> updatedSeats = table.seats().stream()
                .map(seat -> {
                    if (!seat.isOccupied()) {
                        return seat;
                    }
                    Long userId = seat.player().userId();
                    long stack = stackByPlayer.getOrDefault(userId, seat.player().chipStack());
                    TablePlayer updatedPlayer = new TablePlayer(
                            userId,
                            seat.player().nickname(),
                            stack,
                            seat.player().connected(),
                            seat.player().ready()
                    );
                    return new Seat(seat.seatNo(), updatedPlayer);
                })
                .toList();
        Table updatedTable = new Table(table.tableId(), table.name(), table.status(), table.blindPolicy(), updatedSeats, table.dealerSeatNo());
        tableRedisRepository.save(updatedTable);
        tableEventPublisher.publishSnapshot(updatedTable);
        return updatedTable;
    }

    public Table updateDealerButton(String tableId, int dealerSeatNo) {
        Table table = tableRedisRepository.getRequired(tableId);
        Table updatedTable = new Table(table.tableId(), table.name(), table.status(), table.blindPolicy(), table.seats(), dealerSeatNo);
        tableRedisRepository.save(updatedTable);
        tableEventPublisher.publishSnapshot(updatedTable);
        return updatedTable;
    }

    public Table ensureBotOpponents(String tableId, int requestedBotCount, long buyInAmount) {
        Table table = tableRedisRepository.getRequired(tableId);
        long humanCount = table.seats().stream()
                .filter(Seat::isOccupied)
                .filter(seat -> seat.player().chipStack() > 0)
                .filter(seat -> !BotIdentity.isBot(seat.player().userId()))
                .count();
        long botCount = table.seats().stream()
                .filter(Seat::isOccupied)
                .filter(seat -> seat.player().chipStack() > 0)
                .filter(seat -> BotIdentity.isBot(seat.player().userId()))
                .count();
        if (humanCount == 0 || requestedBotCount <= 0) {
            return table;
        }

        int emptySeatCount = (int) table.seats().stream()
                .filter(seat -> !seat.isOccupied() || isBustedBotSeat(seat))
                .count();
        int allowedByCapacity = Math.max(0, emptySeatCount);
        int allowedByLimit = Math.max(0, (int) (11 - humanCount - botCount));
        int addCount = Math.min(requestedBotCount, Math.min(allowedByCapacity, allowedByLimit));
        if (addCount == 0) {
            return table;
        }

        List<Integer> emptySeatNos = table.seats().stream()
                .filter(seat -> !seat.isOccupied() || isBustedBotSeat(seat))
                .map(Seat::seatNo)
                .toList();
        if (emptySeatNos.isEmpty()) {
            throw new BusinessException(ErrorCode.SEAT_NOT_AVAILABLE);
        }

        List<Seat> updatedSeats = new ArrayList<>(table.seats());
        for (int i = 0; i < addCount; i++) {
            int emptySeatNo = emptySeatNos.get(i);
            long botUserId = BOT_SEQUENCE.incrementAndGet();
            updatedSeats.set(
                    emptySeatNo - 1,
                    new Seat(emptySeatNo, new TablePlayer(botUserId, "Player", buyInAmount, true, true))
            );
        }
        Table updatedTable = new Table(table.tableId(), table.name(), table.status(), table.blindPolicy(), updatedSeats, table.dealerSeatNo());
        tableRedisRepository.save(updatedTable);
        tableEventPublisher.publishSnapshot(updatedTable);
        return updatedTable;
    }

    public Table rebuyBustedBots(String tableId, long rebuyAmount) {
        Table table = tableRedisRepository.getRequired(tableId);
        List<Seat> updatedSeats = table.seats().stream()
                .map(seat -> {
                    if (!isBustedBotSeat(seat)) {
                        return seat;
                    }
                    TablePlayer reloadedBot = new TablePlayer(
                            seat.player().userId(),
                            seat.player().nickname(),
                            rebuyAmount,
                            seat.player().connected(),
                            seat.player().ready()
                    );
                    return new Seat(seat.seatNo(), reloadedBot);
                })
                .toList();
        Table updatedTable = new Table(table.tableId(), table.name(), table.status(), table.blindPolicy(), updatedSeats, table.dealerSeatNo());
        tableRedisRepository.save(updatedTable);
        tableEventPublisher.publishSnapshot(updatedTable);
        return updatedTable;
    }

    private boolean isBustedBotSeat(Seat seat) {
        return seat.isOccupied()
                && BotIdentity.isBot(seat.player().userId())
                && seat.player().chipStack() <= 0;
    }
}
