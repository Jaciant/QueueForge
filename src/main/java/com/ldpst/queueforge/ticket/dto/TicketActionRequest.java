package com.ldpst.queueforge.ticket.dto;

import jakarta.validation.constraints.Size;

public record TicketActionRequest(
        @Size(max = 500, message = "Reason must be at most 500 characters")
        String reason
) {
}
