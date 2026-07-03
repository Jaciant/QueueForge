package com.ldpst.queueforge.board.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.ldpst.queueforge.branch.entity.BranchStatus;

public record BranchBoardResponse(
    UUID branchId,
    String branchName,
    BranchStatus branchStatus,
    Instant generatedAt,
    long totalWaitingCount,
    List<QueueServiceBoardResponse> services,
    List<OperatorWindowBoardResponse> windows,
    List<TicketBoardResponse> activeTickets
) {
}
