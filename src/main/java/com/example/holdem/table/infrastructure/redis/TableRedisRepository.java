package com.example.holdem.table.infrastructure.redis;

import com.example.holdem.common.error.BusinessException;
import com.example.holdem.common.error.ErrorCode;
import com.example.holdem.table.domain.Seat;
import com.example.holdem.table.domain.Table;
import com.example.holdem.table.domain.TablePlayer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TableRedisRepository {
    private static final Logger log = LoggerFactory.getLogger(TableRedisRepository.class);
    private static final String TABLE_KEY_PREFIX = "holdem:table:";
    private final ConcurrentHashMap<String, Table> store = new ConcurrentHashMap<>();
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final boolean redisEnabled;

    public TableRedisRepository(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${holdem.redis.enabled:false}") boolean redisEnabled
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.redisEnabled = redisEnabled;
    }

    public Table save(Table table) {
        store.put(table.tableId(), table);
        if (redisEnabled) {
            writeRedis(table);
        }
        return table;
    }

    public Optional<Table> findById(String tableId) {
        if (redisEnabled) {
            Optional<Table> redisValue = readRedis(tableId);
            if (redisValue.isPresent()) {
                return redisValue;
            }
        }
        return Optional.ofNullable(store.get(tableId));
    }

    public List<Table> findAll() {
        if (redisEnabled) {
            try {
                var keys = redisTemplate.keys(TABLE_KEY_PREFIX + "*");
                if (keys != null && !keys.isEmpty()) {
                    List<Table> tables = new ArrayList<>();
                    for (String key : keys) {
                        String payload = redisTemplate.opsForValue().get(key);
                        if (payload != null) {
                            tables.add(fromPayload(payload));
                        }
                    }
                    if (!tables.isEmpty()) {
                        return tables;
                    }
                }
            } catch (Exception exception) {
                log.warn("Redis table scan failed, fallback to memory store", exception);
            }
        }
        return new ArrayList<>(store.values());
    }

    public Table getRequired(String tableId) {
        return findById(tableId).orElseThrow(() -> new BusinessException(ErrorCode.TABLE_NOT_FOUND));
    }

    private void writeRedis(Table table) {
        try {
            redisTemplate.opsForValue().set(TABLE_KEY_PREFIX + table.tableId(), toPayload(table));
        } catch (Exception exception) {
            log.warn("Redis table save failed tableId={}", table.tableId(), exception);
        }
    }

    private Optional<Table> readRedis(String tableId) {
        try {
            String payload = redisTemplate.opsForValue().get(TABLE_KEY_PREFIX + tableId);
            if (payload == null) {
                return Optional.empty();
            }
            Table table = fromPayload(payload);
            store.put(table.tableId(), table);
            return Optional.of(table);
        } catch (Exception exception) {
            log.warn("Redis table read failed tableId={}", tableId, exception);
            return Optional.empty();
        }
    }

    private String toPayload(Table table) throws Exception {
        LinkedHashMap<String, Object> root = new LinkedHashMap<>();
        root.put("tableId", table.tableId());
        root.put("name", table.name());
        root.put("status", table.status().name());
        root.put("smallBlind", table.blindPolicy().smallBlind());
        root.put("bigBlind", table.blindPolicy().bigBlind());
        root.put("ante", table.blindPolicy().ante());
        root.put("dealerSeatNo", table.dealerSeatNo());

        List<LinkedHashMap<String, Object>> seats = new ArrayList<>();
        for (Seat seat : table.seats()) {
            LinkedHashMap<String, Object> seatNode = new LinkedHashMap<>();
            seatNode.put("seatNo", seat.seatNo());
            if (seat.player() != null) {
                TablePlayer player = seat.player();
                LinkedHashMap<String, Object> playerNode = new LinkedHashMap<>();
                playerNode.put("userId", player.userId());
                playerNode.put("nickname", player.nickname());
                playerNode.put("chipStack", player.chipStack());
                playerNode.put("connected", player.connected());
                playerNode.put("ready", player.ready());
                seatNode.put("player", playerNode);
            } else {
                seatNode.put("player", null);
            }
            seats.add(seatNode);
        }
        root.put("seats", seats);
        return objectMapper.writeValueAsString(root);
    }

    private Table fromPayload(String payload) throws Exception {
        LinkedHashMap<String, Object> root = objectMapper.readValue(payload, new TypeReference<>() {
        });
        List<LinkedHashMap<String, Object>> seatNodes = (List<LinkedHashMap<String, Object>>) root.get("seats");
        List<Seat> seats = new ArrayList<>();
        for (LinkedHashMap<String, Object> seatNode : seatNodes) {
            int seatNo = ((Number) seatNode.get("seatNo")).intValue();
            LinkedHashMap<String, Object> playerNode = (LinkedHashMap<String, Object>) seatNode.get("player");
            if (playerNode == null) {
                seats.add(new Seat(seatNo, null));
                continue;
            }
            TablePlayer player = new TablePlayer(
                    ((Number) playerNode.get("userId")).longValue(),
                    (String) playerNode.get("nickname"),
                    ((Number) playerNode.get("chipStack")).longValue(),
                    (Boolean) playerNode.get("connected"),
                    (Boolean) playerNode.get("ready")
            );
            seats.add(new Seat(seatNo, player));
        }
        return new Table(
                (String) root.get("tableId"),
                (String) root.get("name"),
                com.example.holdem.table.domain.TableStatus.valueOf((String) root.get("status")),
                new com.example.holdem.table.domain.BlindPolicy(
                        ((Number) root.get("smallBlind")).longValue(),
                        ((Number) root.get("bigBlind")).longValue(),
                        ((Number) root.get("ante")).longValue()
                ),
                List.copyOf(seats),
                root.get("dealerSeatNo") == null ? null : ((Number) root.get("dealerSeatNo")).intValue()
        );
    }
}
