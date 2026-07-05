package com.ldpst.queueforge.outbox.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "outbox.publisher")
public record OutboxPublisherProperties(
        boolean enabled,
        int batchSize,
        long fixedDelayMs
) {
    private static final int DEFAULT_BATCH_SIZE = 100;
    private static final long DEFAULT_FIXED_DELAY_MS = 5_000;

    public OutboxPublisherProperties {
        if (batchSize <= 0) {
            batchSize = DEFAULT_BATCH_SIZE;
        }

        if (fixedDelayMs <= 0) {
            fixedDelayMs = DEFAULT_FIXED_DELAY_MS;
        }
    }
}
