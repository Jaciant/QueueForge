package com.ldpst.queueforge.ticket.event;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ldpst.queueforge.outbox.service.OutboxEventService;
import com.ldpst.queueforge.ticket.entity.TicketEntity;
import com.ldpst.queueforge.ticket.entity.TicketStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TicketEventPublisher {
    private static final String TICKET_AGGREGATE_TYPE = "Ticket";

    private final OutboxEventService outboxEventService;

    public void publishTicketIssued(TicketEntity ticket, String reason, Instant occurredAt) {
        publish(ticket, TicketEventType.TICKET_ISSUED, null, TicketStatus.WAITING, reason, occurredAt);
    }

    public void publishTicketStatusChanged(
            TicketEntity ticket,
            TicketStatus oldStatus,
            TicketStatus newStatus,
            String reason,
            Instant occurredAt
    ) {
        publish(ticket, TicketEventType.fromNewStatus(newStatus), oldStatus, newStatus, reason, occurredAt);
    }

    private void publish(
            TicketEntity ticket,
            TicketEventType eventType,
            TicketStatus oldStatus,
            TicketStatus newStatus,
            String reason,
            Instant occurredAt
    ) {
        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.put("ticketId", ticket.getId().toString());
        payload.put("branchId", ticket.getBranchId().toString());
        payload.put("serviceId", ticket.getServiceId().toString());
        putNullableUuid(payload, "operatorWindowId", ticket.getOperatorWindowId());
        payload.put("businessDate", ticket.getBusinessDate().toString());
        payload.put("ticketNumber", ticket.getTicketNumber());
        payload.put("sequenceNumber", ticket.getSequenceNumber());
        putNullableStatus(payload, "oldStatus", oldStatus);
        payload.put("newStatus", newStatus.name());
        payload.put("priority", ticket.getPriority());
        payload.put("reason", reason);
        payload.put("occurredAt", occurredAt.toString());

        outboxEventService.saveNewEvent(
                TICKET_AGGREGATE_TYPE,
                ticket.getId(),
                eventType.eventName(),
                payload,
                occurredAt
        );
    }

    private void putNullableUuid(ObjectNode payload, String fieldName, java.util.UUID value) {
        if (value == null) {
            payload.putNull(fieldName);
            return;
        }

        payload.put(fieldName, value.toString());
    }

    private void putNullableStatus(ObjectNode payload, String fieldName, TicketStatus value) {
        if (value == null) {
            payload.putNull(fieldName);
            return;
        }

        payload.put(fieldName, value.name());
    }
}
