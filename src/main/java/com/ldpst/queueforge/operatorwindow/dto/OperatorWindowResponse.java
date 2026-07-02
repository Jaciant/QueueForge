package com.ldpst.queueforge.operatorwindow.dto;

import java.time.Instant;
import java.util.UUID;

import com.ldpst.queueforge.operatorwindow.entity.OperatorWindowStatus;

public record OperatorWindowResponse(
    UUID id,
    UUID branchId,
    Integer number,
    String name,
    OperatorWindowStatus status,
    Instant createdAt,
    Instant updatedAt
) {
}
