package com.example.holdem.game.infrastructure.redis;

import com.example.holdem.game.application.GameStateRepository;
import com.example.holdem.game.domain.Hand;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RedisGameStateRepository implements GameStateRepository {
    private static final Logger log = LoggerFactory.getLogger(RedisGameStateRepository.class);
    private static final String HAND_KEY_PREFIX = "holdem:hand:";
    private final ConcurrentHashMap<String, Hand> store = new ConcurrentHashMap<>();
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final boolean redisEnabled;

    public RedisGameStateRepository(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${holdem.redis.enabled:false}") boolean redisEnabled
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.redisEnabled = redisEnabled;
    }

    @Override
    public Hand save(Hand hand) {
        store.put(hand.tableId(), hand);
        if (redisEnabled) {
            try {
                redisTemplate.opsForValue().set(HAND_KEY_PREFIX + hand.tableId(), objectMapper.writeValueAsString(hand));
            } catch (Exception exception) {
                log.warn("Redis hand save failed tableId={}", hand.tableId(), exception);
            }
        }
        return hand;
    }

    @Override
    public Optional<Hand> findByTableId(String tableId) {
        if (redisEnabled) {
            try {
                String payload = redisTemplate.opsForValue().get(HAND_KEY_PREFIX + tableId);
                if (payload != null) {
                    Hand hand = objectMapper.readValue(payload, Hand.class);
                    store.put(tableId, hand);
                    return Optional.of(hand);
                }
            } catch (Exception exception) {
                log.warn("Redis hand read failed tableId={}", tableId, exception);
            }
        }
        return Optional.ofNullable(store.get(tableId));
    }
}
