package com.ldpst.queueforge.ticket.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ldpst.queueforge.ticket.entity.TicketStatusHistoryEntity;

public interface TicketStatusHistoryRepository extends JpaRepository<TicketStatusHistoryEntity, UUID> {
    List<TicketStatusHistoryEntity> findAllByTicketIdOrderByChangedAtAsc(UUID ticketId);
}
