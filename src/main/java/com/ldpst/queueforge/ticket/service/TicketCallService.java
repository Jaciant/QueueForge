package com.ldpst.queueforge.ticket.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ldpst.queueforge.branch.entity.BranchEntity;
import com.ldpst.queueforge.branch.entity.BranchStatus;
import com.ldpst.queueforge.branch.repository.BranchRepository;
import com.ldpst.queueforge.common.exception.BadRequestException;
import com.ldpst.queueforge.common.exception.ConflictException;
import com.ldpst.queueforge.common.exception.NotFoundException;
import com.ldpst.queueforge.operatorwindow.entity.OperatorWindowEntity;
import com.ldpst.queueforge.operatorwindow.entity.OperatorWindowStatus;
import com.ldpst.queueforge.operatorwindow.repository.OperatorWindowRepository;
import com.ldpst.queueforge.queueservice.entity.QueueServiceEntity;
import com.ldpst.queueforge.queueservice.repository.QueueServiceRepository;
import com.ldpst.queueforge.ticket.dto.TicketResponse;
import com.ldpst.queueforge.ticket.entity.TicketEntity;
import com.ldpst.queueforge.ticket.entity.TicketStatus;
import com.ldpst.queueforge.ticket.entity.TicketStatusHistoryEntity;
import com.ldpst.queueforge.ticket.repository.TicketRepository;
import com.ldpst.queueforge.ticket.repository.TicketStatusHistoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TicketCallService {
    private final TicketRepository ticketRepository;
    private final TicketStatusHistoryRepository ticketStatusHistoryRepository;
    private final OperatorWindowRepository operatorWindowRepository;
    private final BranchRepository branchRepository;
    private final QueueServiceRepository queueServiceRepository;

    @Transactional
    public TicketResponse callNext(UUID windowId, UUID serviceId) {
        OperatorWindowEntity operatorWindow = findOpenOperatorWindowForUpdate(windowId);
        validateActiveBranch(operatorWindow.getBranchId());
        ensureWindowHasNoActiveTicket(operatorWindow.getId());

        if (serviceId != null) {
            validateActiveService(serviceId, operatorWindow.getBranchId());
        }

        TicketEntity ticket = findNextWaitingTicket(operatorWindow.getBranchId(), serviceId)
                .orElseThrow(() -> new NotFoundException("No waiting tickets found"));

        Instant now = Instant.now();
        ticket.setOperatorWindowId(operatorWindow.getId());
        ticket.setStatus(TicketStatus.CALLED);
        ticket.setCalledAt(now);

        TicketEntity savedTicket = ticketRepository.save(ticket);
        saveStatusHistory(savedTicket.getId(), TicketStatus.WAITING, TicketStatus.CALLED, "Ticket called", now);

        return toResponse(savedTicket);
    }

    private OperatorWindowEntity findOpenOperatorWindowForUpdate(UUID windowId) {
        OperatorWindowEntity operatorWindow = operatorWindowRepository.findByIdForUpdate(windowId)
                .orElseThrow(() -> new NotFoundException("Operator window not found"));

        if (operatorWindow.getStatus() != OperatorWindowStatus.OPEN) {
            throw new BadRequestException("Operator window is not open");
        }

        return operatorWindow;
    }

    private void ensureWindowHasNoActiveTicket(UUID windowId) {
        boolean hasActiveTicket = ticketRepository.existsByOperatorWindowIdAndStatusIn(
                windowId,
                List.of(TicketStatus.CALLED, TicketStatus.IN_SERVICE)
        );

        if (hasActiveTicket) {
            throw new ConflictException("Operator window already has active ticket");
        }
    }

    private void validateActiveBranch(UUID branchId) {
        BranchEntity branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new NotFoundException("Branch not found"));

        if (branch.getStatus() != BranchStatus.ACTIVE) {
            throw new BadRequestException("Branch is not active");
        }
    }

    private void validateActiveService(UUID serviceId, UUID branchId) {
        QueueServiceEntity queueService = queueServiceRepository.findById(serviceId)
                .orElseThrow(() -> new NotFoundException("Service not found"));

        if (!queueService.getBranchId().equals(branchId)) {
            throw new BadRequestException("Service does not belong to operator window branch");
        }

        if (!queueService.isActive()) {
            throw new BadRequestException("Service is not active");
        }
    }

    private Optional<TicketEntity> findNextWaitingTicket(UUID branchId, UUID serviceId) {
        if (serviceId == null) {
            return ticketRepository.findNextWaitingForUpdate(branchId);
        }

        return ticketRepository.findNextWaitingByServiceForUpdate(branchId, serviceId);
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
