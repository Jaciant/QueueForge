package com.ldpst.queueforge.outbox.publisher;

import com.ldpst.queueforge.outbox.entity.OutboxEventEntity;

public interface OutboxEventDispatcher {
    void dispatch(OutboxEventEntity event);
}
