package com.ldpst.queueforge.ticket.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

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
}
