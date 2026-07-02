package com.ldpst.queueforge.ticket.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ldpst.queueforge.ticket.entity.TicketEntity;
import com.ldpst.queueforge.ticket.entity.TicketStatus;

public interface TicketRepository extends JpaRepository<TicketEntity, UUID> {
    List<TicketEntity> findAllByBranchIdAndStatusOrderByPriorityDescCreatedAtAsc(
            UUID branchId,
            TicketStatus status
    );

    List<TicketEntity> findAllByBranchIdAndServiceIdAndStatusOrderByPriorityDescCreatedAtAsc(
            UUID branchId,
            UUID serviceId,
            TicketStatus status
    );

    boolean existsByOperatorWindowIdAndStatusIn(UUID operatorWindowId, Collection<TicketStatus> statuses);

    @Query(value = """
            select *
            from tickets
            where branch_id = :branchId
              and status = 'WAITING'
            order by priority desc, created_at asc
            limit 1
            for update skip locked
            """, nativeQuery = true)
    Optional<TicketEntity> findNextWaitingForUpdate(@Param("branchId") UUID branchId);

    @Query(value = """
            select *
            from tickets
            where branch_id = :branchId
              and service_id = :serviceId
              and status = 'WAITING'
            order by priority desc, created_at asc
            limit 1
            for update skip locked
            """, nativeQuery = true)
    Optional<TicketEntity> findNextWaitingByServiceForUpdate(
            @Param("branchId") UUID branchId,
            @Param("serviceId") UUID serviceId
    );
}
