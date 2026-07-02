package com.ldpst.queueforge.queueservice.dto;

import java.time.Instant;
import java.util.UUID;

public record QueueServiceResponse(
    UUID id,
    UUID branchId,
    String code,
    String name,
    String description,
    Integer avgServiceTimeMinutes,
    boolean active,
    Instant createdAt,
    Instant updatedAt
) {
}
