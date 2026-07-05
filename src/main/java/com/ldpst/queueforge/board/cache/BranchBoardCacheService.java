package com.ldpst.queueforge.board.cache;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ldpst.queueforge.board.dto.BranchBoardResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class BranchBoardCacheService {
    private static final String KEY_PREFIX = "queueforge:branch-board:";

    private final StringRedisTemplate redisTemplate;
    private final BranchBoardCacheProperties properties;
    private final ObjectMapper objectMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    public Optional<BranchBoardResponse> get(UUID branchId) {
        if (!properties.enabled()) {
            return Optional.empty();
        }

        try {
            String value = redisTemplate.opsForValue().get(key(branchId));
            if (value == null) {
                return Optional.empty();
            }

            return Optional.of(objectMapper.readValue(value, BranchBoardResponse.class));
        } catch (RuntimeException | JsonProcessingException exception) {
            log.warn("Failed to read branch board cache for branchId={}", branchId, exception);
            return Optional.empty();
        }
    }

    public void put(UUID branchId, BranchBoardResponse response) {
        if (!properties.enabled()) {
            return;
        }

        try {
            String value = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(key(branchId), value, properties.ttl());
        } catch (RuntimeException | JsonProcessingException exception) {
            log.warn("Failed to write branch board cache for branchId={}", branchId, exception);
        }
    }

    public void evict(UUID branchId) {
        if (!properties.enabled()) {
            return;
        }

        try {
            redisTemplate.delete(key(branchId));
        } catch (RuntimeException exception) {
            log.warn("Failed to evict branch board cache for branchId={}", branchId, exception);
        }
    }

    private String key(UUID branchId) {
        return KEY_PREFIX + branchId;
    }
}
