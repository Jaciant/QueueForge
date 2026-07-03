package com.ldpst.queueforge.board.dto;

import java.util.List;
import java.util.UUID;

import com.ldpst.queueforge.operatorwindow.entity.OperatorWindowStatus;

public record OperatorWindowBoardResponse(
    UUID windowId,
    Integer number,
    String name,
    OperatorWindowStatus status,
    List<AssignedQueueServiceBoardResponse> assignedServices,
    TicketBoardResponse currentTicket
) {
}
