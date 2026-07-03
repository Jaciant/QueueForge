package com.ldpst.queueforge.board.dto;

import java.util.UUID;

public record AssignedQueueServiceBoardResponse(
    UUID serviceId,
    String serviceCode,
    String serviceName,
    boolean active
) {
}
