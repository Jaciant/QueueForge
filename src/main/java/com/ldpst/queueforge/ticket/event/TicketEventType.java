package com.ldpst.queueforge.ticket.event;

import com.ldpst.queueforge.ticket.entity.TicketStatus;

public enum TicketEventType {
    TICKET_ISSUED("TicketIssued"),
    TICKET_CALLED("TicketCalled"),
    TICKET_SERVICE_STARTED("TicketServiceStarted"),
    TICKET_COMPLETED("TicketCompleted"),
    TICKET_SKIPPED("TicketSkipped"),
    TICKET_CANCELLED("TicketCancelled");

    private final String eventName;

    TicketEventType(String eventName) {
        this.eventName = eventName;
    }

    public String eventName() {
        return eventName;
    }

    public static TicketEventType fromNewStatus(TicketStatus status) {
        return switch (status) {
            case CALLED -> TICKET_CALLED;
            case IN_SERVICE -> TICKET_SERVICE_STARTED;
            case COMPLETED -> TICKET_COMPLETED;
            case SKIPPED -> TICKET_SKIPPED;
            case CANCELLED -> TICKET_CANCELLED;
            default -> throw new IllegalArgumentException("Unsupported ticket event status: " + status);
        };
    }
}
