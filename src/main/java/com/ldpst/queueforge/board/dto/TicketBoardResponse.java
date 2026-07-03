package com.ldpst.queueforge.board.dto;

import java.time.Instant;
import java.util.UUID;

import com.ldpst.queueforge.ticket.entity.TicketStatus;

public record TicketBoardResponse(
    UUID id,
    UUID serviceId,
    String serviceCode,
    UUID operatorWindowId,
    Integer operatorWindowNumber,
    String ticketNumber,
    TicketStatus status,
    Integer priority,
    Instant createdAt,
    Instant calledAt,
    Instant serviceStartedAt
) {
}
