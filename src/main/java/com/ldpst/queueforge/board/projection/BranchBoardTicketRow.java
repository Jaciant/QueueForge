package com.ldpst.queueforge.board.projection;

import java.time.Instant;
import java.util.UUID;

import com.ldpst.queueforge.ticket.entity.TicketStatus;

public record BranchBoardTicketRow(
    UUID id,
    UUID serviceId,
    UUID operatorWindowId,
    String ticketNumber,
    TicketStatus status,
    Integer priority,
    Instant createdAt,
    Instant calledAt,
    Instant serviceStartedAt
) {
}
