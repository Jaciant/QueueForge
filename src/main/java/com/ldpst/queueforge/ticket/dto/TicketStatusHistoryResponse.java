package com.ldpst.queueforge.ticket.dto;

import java.time.Instant;
import java.util.UUID;

import com.ldpst.queueforge.ticket.entity.TicketStatus;

public record TicketStatusHistoryResponse(
        UUID id,
        UUID ticketId,
        TicketStatus oldStatus,
        TicketStatus newStatus,
        UUID changedBy,
        String reason,
        Instant changedAt
) {
}
