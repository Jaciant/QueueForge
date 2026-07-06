package com.ldpst.queueforge.outbox.publisher;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ldpst.queueforge.outbox.config.OutboxPublisherProperties;
import com.ldpst.queueforge.outbox.entity.OutboxEventEntity;
import com.ldpst.queueforge.outbox.entity.OutboxEventStatus;
import com.ldpst.queueforge.outbox.repository.OutboxEventRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OutboxEventPublisher {
    private final OutboxEventRepository outboxEventRepository;
    private final OutboxEventDispatcher outboxEventDispatcher;
    private final OutboxPublisherProperties properties;

    @Transactional
    public int publishBatch(int batchSize) {
        List<OutboxEventEntity> events = outboxEventRepository.findPublishableEventsForUpdate(
                properties.maxRetries(),
                batchSize
        );

        for (OutboxEventEntity event : events) {
            publishSingleEvent(event);
        }

        return events.size();
    }

    private void publishSingleEvent(OutboxEventEntity event) {
        try {
            outboxEventDispatcher.dispatch(event);
            event.setStatus(OutboxEventStatus.PUBLISHED);
            event.setPublishedAt(Instant.now());
            event.setLastError(null);
        } catch (RuntimeException exception) {
            event.setStatus(OutboxEventStatus.FAILED);
            event.setRetryCount(resolveRetryCount(event) + 1);
            event.setLastError(exception.getMessage());
        }
    }

    private int resolveRetryCount(OutboxEventEntity event) {
        if (event.getRetryCount() == null) {
            return 0;
        }

        return event.getRetryCount();
    }
}
