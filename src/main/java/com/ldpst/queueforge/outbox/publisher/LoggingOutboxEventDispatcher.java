package com.ldpst.queueforge.outbox.publisher;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.ldpst.queueforge.outbox.entity.OutboxEventEntity;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnProperty(
        prefix = "outbox.publisher",
        name = "dispatcher",
        havingValue = "logging",
        matchIfMissing = true
)
public class LoggingOutboxEventDispatcher implements OutboxEventDispatcher {
    @Override
    public void dispatch(OutboxEventEntity event) {
        log.info(
                "Outbox event dispatched: id={}, eventType={}, aggregateType={}, aggregateId={}",
                event.getId(),
                event.getEventType(),
                event.getAggregateType(),
                event.getAggregateId()
        );
    }
}
