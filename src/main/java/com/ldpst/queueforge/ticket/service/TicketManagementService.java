package com.ldpst.queueforge.ticket.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ldpst.queueforge.branch.entity.BranchEntity;
import com.ldpst.queueforge.branch.entity.BranchStatus;
import com.ldpst.queueforge.branch.repository.BranchRepository;
import com.ldpst.queueforge.common.exception.BadRequestException;
import com.ldpst.queueforge.common.exception.NotFoundException;
import com.ldpst.queueforge.queueservice.entity.QueueServiceEntity;
import com.ldpst.queueforge.queueservice.repository.QueueServiceRepository;
import com.ldpst.queueforge.ticket.dto.CreateTicketRequest;
import com.ldpst.queueforge.ticket.dto.TicketResponse;
import com.ldpst.queueforge.ticket.entity.TicketEntity;
import com.ldpst.queueforge.ticket.entity.TicketStatus;
import com.ldpst.queueforge.ticket.entity.TicketStatusHistoryEntity;
import com.ldpst.queueforge.ticket.repository.TicketRepository;
import com.ldpst.queueforge.ticket.repository.TicketStatusHistoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TicketManagementService {
    private static final int DEFAULT_PRIORITY = 0;

    private final TicketRepository ticketRepository;
    private final TicketStatusHistoryRepository ticketStatusHistoryRepository;
    private final BranchRepository branchRepository;
    private final QueueServiceRepository queueServiceRepository;
    private final TicketNumberGenerator ticketNumberGenerator;

    @Transactional
    public TicketResponse create(CreateTicketRequest request) {
        BranchEntity branch = findActiveBranch(request.branchId());
        QueueServiceEntity queueService = findActiveQueueService(request.serviceId(), branch.getId());

        Instant now = Instant.now();
        LocalDate businessDate = LocalDate.now(ZoneId.of(branch.getTimezone()));
        int priority = request.priority() == null ? DEFAULT_PRIORITY : request.priority();

        TicketNumberGenerator.GeneratedTicketNumber generatedTicketNumber = ticketNumberGenerator.generate(
                branch.getId(),
                queueService.getId(),
                businessDate,
                queueService.getCode(),
                now
        );

        TicketEntity ticket = new TicketEntity();
        ticket.setBranchId(branch.getId());
        ticket.setServiceId(queueService.getId());
        ticket.setBusinessDate(businessDate);
        ticket.setTicketNumber(generatedTicketNumber.ticketNumber());
        ticket.setSequenceNumber(generatedTicketNumber.sequenceNumber());
        ticket.setStatus(TicketStatus.WAITING);
        ticket.setPriority(priority);
        ticket.setCreatedAt(now);

        TicketEntity savedTicket = ticketRepository.save(ticket);
        saveStatusHistory(savedTicket.getId(), null, TicketStatus.WAITING, "Ticket created", now);

        return toResponse(savedTicket);
    }

    @Transactional(readOnly = true)
    public TicketResponse getById(UUID ticketId) {
        TicketEntity ticket = findTicket(ticketId);

        return toResponse(ticket);
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> getWaitingByBranch(UUID branchId, UUID serviceId) {
        if (!branchRepository.existsById(branchId)) {
            throw new NotFoundException("Branch not found");
        }

        if (serviceId != null) {
            QueueServiceEntity queueService = queueServiceRepository.findById(serviceId)
                    .orElseThrow(() -> new NotFoundException("Service not found"));

            if (!queueService.getBranchId().equals(branchId)) {
                throw new BadRequestException("Service does not belong to branch");
            }

            return ticketRepository.findAllByBranchIdAndServiceIdAndStatusOrderByPriorityDescCreatedAtAsc(
                            branchId,
                            serviceId,
                            TicketStatus.WAITING
                    )
                    .stream()
                    .map(ticket -> toResponse(ticket))
                    .toList();
        }

        return ticketRepository.findAllByBranchIdAndStatusOrderByPriorityDescCreatedAtAsc(branchId, TicketStatus.WAITING)
                .stream()
                .map(ticket -> toResponse(ticket))
                .toList();
    }

    private BranchEntity findActiveBranch(UUID branchId) {
        BranchEntity branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new NotFoundException("Branch not found"));

        if (branch.getStatus() != BranchStatus.ACTIVE) {
            throw new BadRequestException("Branch is not active");
        }

        return branch;
    }

    private QueueServiceEntity findActiveQueueService(UUID serviceId, UUID branchId) {
        QueueServiceEntity queueService = queueServiceRepository.findById(serviceId)
                .orElseThrow(() -> new NotFoundException("Service not found"));

        if (!queueService.getBranchId().equals(branchId)) {
            throw new BadRequestException("Service does not belong to branch");
        }

        if (!queueService.isActive()) {
            throw new BadRequestException("Service is not active");
        }

        return queueService;
    }

    private TicketEntity findTicket(UUID ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new NotFoundException("Ticket not found"));
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
