package com.ldpst.queueforge;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ldpst.queueforge.outbox.entity.OutboxEventEntity;
import com.ldpst.queueforge.outbox.entity.OutboxEventStatus;
import com.ldpst.queueforge.outbox.publisher.OutboxEventPublisher;
import com.ldpst.queueforge.outbox.repository.OutboxEventRepository;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class OutboxPublisherIntegrationTest {
    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private OutboxEventPublisher outboxEventPublisher;

    @Test
    void publishBatchPublishesOnlyRequestedNumberOfNewEvents() {
        outboxEventRepository.deleteAll();

        UUID aggregateId = UUID.randomUUID();
        OutboxEventEntity firstEvent = saveNewEvent(aggregateId, "TicketIssued", Instant.parse("2026-01-01T10:00:00Z"));
        OutboxEventEntity secondEvent = saveNewEvent(aggregateId, "TicketCalled", Instant.parse("2026-01-01T10:00:01Z"));
        OutboxEventEntity thirdEvent = saveNewEvent(aggregateId, "TicketCompleted", Instant.parse("2026-01-01T10:00:02Z"));

        int publishedCount = outboxEventPublisher.publishBatch(2);

        assertThat(publishedCount).isEqualTo(2);

        List<OutboxEventEntity> events = outboxEventRepository
                .findAllByAggregateTypeAndAggregateIdOrderByCreatedAtAsc("Ticket", aggregateId);
        assertThat(events).extracting(OutboxEventEntity::getId)
                .containsExactly(firstEvent.getId(), secondEvent.getId(), thirdEvent.getId());
        assertThat(events).extracting(OutboxEventEntity::getStatus)
                .containsExactly(OutboxEventStatus.PUBLISHED, OutboxEventStatus.PUBLISHED, OutboxEventStatus.NEW);
        assertThat(events.get(0).getPublishedAt()).isNotNull();
        assertThat(events.get(1).getPublishedAt()).isNotNull();
        assertThat(events.get(2).getPublishedAt()).isNull();
    }

    private OutboxEventEntity saveNewEvent(UUID aggregateId, String eventType, Instant createdAt) {
        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.put("eventType", eventType);

        OutboxEventEntity event = new OutboxEventEntity();
        event.setAggregateType("Ticket");
        event.setAggregateId(aggregateId);
        event.setEventType(eventType);
        event.setPayload(payload);
        event.setStatus(OutboxEventStatus.NEW);
        event.setRetryCount(0);
        event.setCreatedAt(createdAt);

        return outboxEventRepository.save(event);
    }
}
