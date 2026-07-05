package com.ldpst.queueforge.outbox.publisher;

import java.util.concurrent.TimeUnit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ldpst.queueforge.outbox.config.OutboxPublisherProperties;
import com.ldpst.queueforge.outbox.entity.OutboxEventEntity;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "outbox.publisher", name = "dispatcher", havingValue = "kafka")
public class KafkaOutboxEventDispatcher implements OutboxEventDispatcher {
    private static final long SEND_TIMEOUT_SECONDS = 10;

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxPublisherProperties properties;

    @Override
    public void dispatch(OutboxEventEntity event) {
        try {
            kafkaTemplate.send(properties.topic(), event.getAggregateId().toString(), toEnvelope(event))
                    .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to dispatch outbox event to Kafka", exception);
        }
    }

    private String toEnvelope(OutboxEventEntity event) {
        ObjectNode envelope = JsonNodeFactory.instance.objectNode();
        envelope.put("eventId", event.getId().toString());
        envelope.put("eventType", event.getEventType());
        envelope.put("aggregateType", event.getAggregateType());
        envelope.put("aggregateId", event.getAggregateId().toString());
        envelope.put("createdAt", event.getCreatedAt().toString());
        envelope.set("payload", event.getPayload());

        return envelope.toString();
    }
}
