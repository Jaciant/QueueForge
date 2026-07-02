package com.ldpst.queueforge.ticket.dto;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateTicketRequest(
    @NotNull(message = "Branch id must not be null")
    UUID branchId,

    @NotNull(message = "Service id must not be null")
    UUID serviceId,

    @Min(value = 0, message = "Priority must be greater than or equal to 0")
    Integer priority
) {
}
