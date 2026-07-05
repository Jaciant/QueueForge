package com.ldpst.queueforge.outbox.publisher;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ldpst.queueforge.outbox.config.OutboxPublisherProperties;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "outbox.publisher", name = "enabled", havingValue = "true")
public class OutboxEventPublisherScheduler {
    private final OutboxEventPublisher outboxEventPublisher;
    private final OutboxPublisherProperties properties;

    @Scheduled(fixedDelayString = "${outbox.publisher.fixed-delay-ms:5000}")
    public void publishNewEvents() {
        outboxEventPublisher.publishBatch(properties.batchSize());
    }
}
