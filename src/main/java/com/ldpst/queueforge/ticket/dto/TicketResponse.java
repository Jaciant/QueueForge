package com.ldpst.queueforge.ticket.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.ldpst.queueforge.ticket.entity.TicketStatus;

public record TicketResponse(
    UUID id,
    UUID branchId,
    UUID serviceId,
    UUID operatorWindowId,
    LocalDate businessDate,
    String ticketNumber,
    Integer sequenceNumber,
    TicketStatus status,
    Integer priority,
    Instant createdAt,
    Instant calledAt,
    Instant serviceStartedAt,
    Instant completedAt,
    Instant skippedAt,
    Instant cancelledAt,
    Instant expiredAt
) {
}
