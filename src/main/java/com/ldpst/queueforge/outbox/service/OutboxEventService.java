package com.ldpst.queueforge.outbox.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ldpst.queueforge.outbox.entity.OutboxEventEntity;
import com.ldpst.queueforge.outbox.entity.OutboxEventStatus;
import com.ldpst.queueforge.outbox.repository.OutboxEventRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OutboxEventService {
    private final OutboxEventRepository outboxEventRepository;

    public OutboxEventEntity saveNewEvent(
            String aggregateType,
            UUID aggregateId,
            String eventType,
            JsonNode payload,
            Instant createdAt
    ) {
        OutboxEventEntity event = new OutboxEventEntity();
        event.setAggregateType(aggregateType);
        event.setAggregateId(aggregateId);
        event.setEventType(eventType);
        event.setPayload(payload);
        event.setStatus(OutboxEventStatus.NEW);
        event.setRetryCount(0);
        event.setCreatedAt(createdAt);

        return outboxEventRepository.save(event);
    }
}
