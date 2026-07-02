package com.ldpst.queueforge.ticket.service;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ldpst.queueforge.common.exception.BadRequestException;
import com.ldpst.queueforge.common.exception.NotFoundException;
import com.ldpst.queueforge.ticket.dto.TicketResponse;
import com.ldpst.queueforge.ticket.dto.TicketStatusHistoryResponse;
import com.ldpst.queueforge.ticket.entity.TicketEntity;
import com.ldpst.queueforge.ticket.entity.TicketStatus;
import com.ldpst.queueforge.ticket.entity.TicketStatusHistoryEntity;
import com.ldpst.queueforge.ticket.repository.TicketRepository;
import com.ldpst.queueforge.ticket.repository.TicketStatusHistoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TicketLifecycleService {
    private final TicketRepository ticketRepository;
    private final TicketStatusHistoryRepository ticketStatusHistoryRepository;

    @Transactional
    public TicketResponse startService(UUID ticketId, String reason) {
        TicketEntity ticket = findTicketForUpdate(ticketId);
        ensureCurrentStatus(ticket, TicketStatus.CALLED);
        ensureTicketHasOperatorWindow(ticket);

        Instant now = Instant.now();
        TicketStatus oldStatus = ticket.getStatus();
        ticket.setStatus(TicketStatus.IN_SERVICE);
        ticket.setServiceStartedAt(now);

        TicketEntity savedTicket = ticketRepository.save(ticket);
        saveStatusHistory(savedTicket.getId(), oldStatus, TicketStatus.IN_SERVICE, normalizeReason(reason, "Service started"), now);

        return toResponse(savedTicket);
    }

    @Transactional
    public TicketResponse complete(UUID ticketId, String reason) {
        TicketEntity ticket = findTicketForUpdate(ticketId);
        ensureCurrentStatus(ticket, TicketStatus.IN_SERVICE);

        Instant now = Instant.now();
        TicketStatus oldStatus = ticket.getStatus();
        ticket.setStatus(TicketStatus.COMPLETED);
        ticket.setCompletedAt(now);

        TicketEntity savedTicket = ticketRepository.save(ticket);
        saveStatusHistory(savedTicket.getId(), oldStatus, TicketStatus.COMPLETED, normalizeReason(reason, "Service completed"), now);

        return toResponse(savedTicket);
    }

    @Transactional
    public TicketResponse skip(UUID ticketId, String reason) {
        TicketEntity ticket = findTicketForUpdate(ticketId);
        ensureCurrentStatus(ticket, TicketStatus.CALLED);
        ensureTicketHasOperatorWindow(ticket);

        Instant now = Instant.now();
        TicketStatus oldStatus = ticket.getStatus();
        ticket.setStatus(TicketStatus.SKIPPED);
        ticket.setSkippedAt(now);

        TicketEntity savedTicket = ticketRepository.save(ticket);
        saveStatusHistory(savedTicket.getId(), oldStatus, TicketStatus.SKIPPED, normalizeReason(reason, "Ticket skipped"), now);

        return toResponse(savedTicket);
    }

    @Transactional
    public TicketResponse cancel(UUID ticketId, String reason) {
        TicketEntity ticket = findTicketForUpdate(ticketId);
        ensureCurrentStatusIn(ticket, Set.of(TicketStatus.WAITING, TicketStatus.CALLED));

        Instant now = Instant.now();
        TicketStatus oldStatus = ticket.getStatus();
        ticket.setStatus(TicketStatus.CANCELLED);
        ticket.setCancelledAt(now);

        TicketEntity savedTicket = ticketRepository.save(ticket);
        saveStatusHistory(savedTicket.getId(), oldStatus, TicketStatus.CANCELLED, normalizeReason(reason, "Ticket cancelled"), now);

        return toResponse(savedTicket);
    }

    @Transactional(readOnly = true)
    public List<TicketStatusHistoryResponse> getHistory(UUID ticketId) {
        if (!ticketRepository.existsById(ticketId)) {
            throw new NotFoundException("Ticket not found");
        }

        return ticketStatusHistoryRepository.findAllByTicketIdOrderByChangedAtAsc(ticketId)
                .stream()
                .map(history -> new TicketStatusHistoryResponse(
                        history.getId(),
                        history.getTicketId(),
                        history.getOldStatus(),
                        history.getNewStatus(),
                        history.getChangedBy(),
                        history.getReason(),
                        history.getChangedAt()
                ))
                .toList();
    }

    private TicketEntity findTicketForUpdate(UUID ticketId) {
        return ticketRepository.findByIdForUpdate(ticketId)
                .orElseThrow(() -> new NotFoundException("Ticket not found"));
    }

    private void ensureCurrentStatus(TicketEntity ticket, TicketStatus expectedStatus) {
        if (ticket.getStatus() != expectedStatus) {
            throw new BadRequestException(
                    "Ticket status must be " + expectedStatus + ", but current status is " + ticket.getStatus()
            );
        }
    }

    private void ensureCurrentStatusIn(TicketEntity ticket, Set<TicketStatus> allowedStatuses) {
        if (!allowedStatuses.contains(ticket.getStatus())) {
            throw new BadRequestException(
                    "Ticket status must be one of " + allowedStatuses + ", but current status is " + ticket.getStatus()
            );
        }
    }

    private void ensureTicketHasOperatorWindow(TicketEntity ticket) {
        if (ticket.getOperatorWindowId() == null) {
            throw new BadRequestException("Ticket is not assigned to operator window");
        }
    }

    private String normalizeReason(String reason, String defaultReason) {
        if (reason == null || reason.isBlank()) {
            return defaultReason;
        }

        return reason.trim();
    }

    private void saveStatusHistory(
            UUID ticketId,
            TicketStatus oldStatus,
            TicketStatus newStatus,
            String reason,
            Instant changedAt
    ) {
        TicketStatusHistoryEntity history = new TicketStatusHistoryEntity();
        history.setTicketId(ticketId);
        history.setOldStatus(oldStatus);
        history.setNewStatus(newStatus);
        history.setReason(reason);
        history.setChangedAt(changedAt);

        ticketStatusHistoryRepository.save(history);
    }

    private TicketResponse toResponse(TicketEntity entity) {
        return new TicketResponse(
                entity.getId(),
                entity.getBranchId(),
                entity.getServiceId(),
                entity.getOperatorWindowId(),
                entity.getBusinessDate(),
                entity.getTicketNumber(),
                entity.getSequenceNumber(),
                entity.getStatus(),
                entity.getPriority(),
                entity.getCreatedAt(),
                entity.getCalledAt(),
                entity.getServiceStartedAt(),
                entity.getCompletedAt(),
                entity.getSkippedAt(),
                entity.getCancelledAt(),
                entity.getExpiredAt()
        );
    }
}
