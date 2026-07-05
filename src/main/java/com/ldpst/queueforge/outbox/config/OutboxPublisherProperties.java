package com.ldpst.queueforge.outbox.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "outbox.publisher")
public record OutboxPublisherProperties(
        boolean enabled,
        int batchSize,
        long fixedDelayMs,
        String dispatcher,
        String topic
) {
    private static final int DEFAULT_BATCH_SIZE = 100;
    private static final long DEFAULT_FIXED_DELAY_MS = 5_000;
    private static final String DEFAULT_DISPATCHER = "logging";
    private static final String DEFAULT_TOPIC = "queueforge.ticket-events";

    public OutboxPublisherProperties {
        if (batchSize <= 0) {
            batchSize = DEFAULT_BATCH_SIZE;
        }

        if (fixedDelayMs <= 0) {
            fixedDelayMs = DEFAULT_FIXED_DELAY_MS;
        }

        if (dispatcher == null || dispatcher.isBlank()) {
            dispatcher = DEFAULT_DISPATCHER;
        }

        if (topic == null || topic.isBlank()) {
            topic = DEFAULT_TOPIC;
        }
    }
}
