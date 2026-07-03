package com.ldpst.queueforge.board.dto;

import java.util.UUID;

public record QueueServiceBoardResponse(
    UUID serviceId,
    String serviceCode,
    String serviceName,
    boolean active,
    long waitingCount,
    TicketBoardResponse nextWaitingTicket
) {
}
