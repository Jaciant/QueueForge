package com.ldpst.queueforge.outbox.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ldpst.queueforge.outbox.entity.OutboxEventEntity;
import com.ldpst.queueforge.outbox.entity.OutboxEventStatus;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {
    List<OutboxEventEntity> findAllByAggregateTypeAndAggregateIdOrderByCreatedAtAsc(
            String aggregateType,
            UUID aggregateId
    );

    List<OutboxEventEntity> findTop100ByStatusOrderByCreatedAtAsc(OutboxEventStatus status);
}
