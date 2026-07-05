package com.ldpst.queueforge.outbox.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ldpst.queueforge.outbox.entity.OutboxEventEntity;
import com.ldpst.queueforge.outbox.entity.OutboxEventStatus;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {
    List<OutboxEventEntity> findAllByAggregateTypeAndAggregateIdOrderByCreatedAtAsc(
            String aggregateType,
            UUID aggregateId
    );

    List<OutboxEventEntity> findTop100ByStatusOrderByCreatedAtAsc(OutboxEventStatus status);

    @Query(value = """
            select *
            from outbox_events
            where status = 'NEW'
            order by created_at asc
            limit :batchSize
            for update skip locked
            """, nativeQuery = true)
    List<OutboxEventEntity> findNewEventsForUpdate(@Param("batchSize") int batchSize);
}
