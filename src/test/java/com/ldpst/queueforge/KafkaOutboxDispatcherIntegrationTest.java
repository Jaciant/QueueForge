package com.ldpst.queueforge;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ldpst.queueforge.outbox.entity.OutboxEventEntity;
import com.ldpst.queueforge.outbox.entity.OutboxEventStatus;
import com.ldpst.queueforge.outbox.publisher.OutboxEventPublisher;
import com.ldpst.queueforge.outbox.repository.OutboxEventRepository;

@Testcontainers
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
        "outbox.publisher.enabled=false",
        "outbox.publisher.dispatcher=kafka",
        "outbox.publisher.topic=queueforge.test-ticket-events"
})
class KafkaOutboxDispatcherIntegrationTest {
    private static final String TOPIC = "queueforge.test-ticket-events";

    @Container
    private static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka-native:3.8.0");

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private OutboxEventPublisher outboxEventPublisher;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

    @Test
    void publishBatchSendsOutboxEventToKafkaAndMarksItPublished() {
        outboxEventRepository.deleteAll();

        UUID aggregateId = UUID.randomUUID();
        OutboxEventEntity event = saveNewEvent(aggregateId);

        int publishedCount = outboxEventPublisher.publishBatch(1);

        assertThat(publishedCount).isEqualTo(1);

        OutboxEventEntity publishedEvent = outboxEventRepository.findById(event.getId()).orElseThrow();
        assertThat(publishedEvent.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
        assertThat(publishedEvent.getPublishedAt()).isNotNull();

        JsonNode message = readSingleKafkaMessage();
        assertThat(message.get("eventId").asText()).isEqualTo(event.getId().toString());
        assertThat(message.get("eventType").asText()).isEqualTo("TicketIssued");
        assertThat(message.get("aggregateType").asText()).isEqualTo("Ticket");
        assertThat(message.get("aggregateId").asText()).isEqualTo(aggregateId.toString());
        assertThat(message.get("payload").get("ticketId").asText()).isEqualTo(aggregateId.toString());
        assertThat(message.get("payload").get("newStatus").asText()).isEqualTo("WAITING");
    }

    private OutboxEventEntity saveNewEvent(UUID aggregateId) {
        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.put("ticketId", aggregateId.toString());
        payload.put("newStatus", "WAITING");

        OutboxEventEntity event = new OutboxEventEntity();
        event.setAggregateType("Ticket");
        event.setAggregateId(aggregateId);
        event.setEventType("TicketIssued");
        event.setPayload(payload);
        event.setStatus(OutboxEventStatus.NEW);
        event.setRetryCount(0);
        event.setCreatedAt(Instant.now());

        return outboxEventRepository.save(event);
    }

    private JsonNode readSingleKafkaMessage() {
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProperties())) {
            consumer.subscribe(List.of(TOPIC));

            ConsumerRecords<String, String> records = pollUntilMessageArrives(consumer);
            assertThat(records.count()).isGreaterThanOrEqualTo(1);

            return parse(records.iterator().next().value());
        }
    }

    private ConsumerRecords<String, String> pollUntilMessageArrives(KafkaConsumer<String, String> consumer) {
        Instant deadline = Instant.now().plusSeconds(10);

        while (Instant.now().isBefore(deadline)) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
            if (!records.isEmpty()) {
                return records;
            }
        }

        return ConsumerRecords.empty();
    }

    private Map<String, Object> consumerProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "queueforge-outbox-test-" + UUID.randomUUID());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return properties;
    }

    private JsonNode parse(String value) {
        try {
            return objectMapper.readTree(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to parse Kafka message: " + value, exception);
        }
    }
}
