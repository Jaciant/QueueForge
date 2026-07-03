package com.ldpst.queueforge.board.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ldpst.queueforge.board.dto.BranchBoardResponse;
import com.ldpst.queueforge.board.dto.OperatorWindowBoardResponse;
import com.ldpst.queueforge.board.dto.QueueServiceBoardResponse;
import com.ldpst.queueforge.board.dto.TicketBoardResponse;
import com.ldpst.queueforge.branch.entity.BranchEntity;
import com.ldpst.queueforge.branch.repository.BranchRepository;
import com.ldpst.queueforge.common.exception.NotFoundException;
import com.ldpst.queueforge.operatorwindow.entity.OperatorWindowEntity;
import com.ldpst.queueforge.operatorwindow.repository.OperatorWindowRepository;
import com.ldpst.queueforge.queueservice.entity.QueueServiceEntity;
import com.ldpst.queueforge.queueservice.repository.QueueServiceRepository;
import com.ldpst.queueforge.board.repository.BranchBoardQueryRepository;
import com.ldpst.queueforge.board.projection.BranchBoardTicketRow;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BranchBoardService {
    private final BranchRepository branchRepository;
    private final QueueServiceRepository queueServiceRepository;
    private final OperatorWindowRepository operatorWindowRepository;
    private final BranchBoardQueryRepository branchBoardQueryRepository;

    @Transactional(readOnly = true)
    public BranchBoardResponse getBoard(UUID branchId) {
        BranchEntity branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new NotFoundException("Branch not found"));

        List<QueueServiceEntity> queueServices = queueServiceRepository.findAllByBranchIdOrderByCreatedAtAsc(branchId);
        List<OperatorWindowEntity> operatorWindows = operatorWindowRepository.findAllByBranchIdOrderByNumberAsc(branchId);
        List<BranchBoardTicketRow> activeTickets = branchBoardQueryRepository.getActiveTickets(branchId);

        Map<UUID, QueueServiceEntity> serviceById = queueServices.stream()
                .collect(Collectors.toMap(QueueServiceEntity::getId, Function.identity()));

        Map<UUID, OperatorWindowEntity> windowById = operatorWindows.stream()
                .collect(Collectors.toMap(OperatorWindowEntity::getId, Function.identity()));

        Map<UUID, Long> waitingCountByServiceId = branchBoardQueryRepository.getWaitingCountByService(branchId);
        Map<UUID, BranchBoardTicketRow> nextWaitingTicketByServiceId = branchBoardQueryRepository.getNextWaitingTicketByService(branchId);

        Map<UUID, BranchBoardTicketRow> activeTicketByWindowId = activeTickets.stream()
                .filter(ticket -> ticket.operatorWindowId() != null)
                .collect(Collectors.toMap(
                        BranchBoardTicketRow::operatorWindowId,
                        Function.identity(),
                        (first, second) -> first
                ));

        List<QueueServiceBoardResponse> serviceResponses = queueServices.stream()
                .map(queueService -> toQueueServiceResponse(
                        queueService,
                        waitingCountByServiceId.getOrDefault(queueService.getId(), 0L),
                        nextWaitingTicketByServiceId.get(queueService.getId()),
                        serviceById,
                        windowById
                ))
                .toList();

        List<OperatorWindowBoardResponse> windowResponses = operatorWindows.stream()
                .map(operatorWindow -> toOperatorWindowResponse(
                        operatorWindow,
                        activeTicketByWindowId.get(operatorWindow.getId()),
                        serviceById,
                        windowById
                ))
                .toList();

        List<TicketBoardResponse> activeTicketResponses = activeTickets.stream()
                .map(ticket -> toTicketResponse(ticket, serviceById, windowById))
                .toList();

        long totalWaitingCount = waitingCountByServiceId.values()
                .stream()
                .mapToLong(Long::longValue)
                .sum();

        return new BranchBoardResponse(
                branch.getId(),
                branch.getName(),
                branch.getStatus(),
                Instant.now(),
                totalWaitingCount,
                serviceResponses,
                windowResponses,
                activeTicketResponses
        );
    }

    private QueueServiceBoardResponse toQueueServiceResponse(
            QueueServiceEntity queueService,
            long waitingCount,
            BranchBoardTicketRow nextWaitingTicket,
            Map<UUID, QueueServiceEntity> serviceById,
            Map<UUID, OperatorWindowEntity> windowById
    ) {
        return new QueueServiceBoardResponse(
                queueService.getId(),
                queueService.getCode(),
                queueService.getName(),
                queueService.isActive(),
                waitingCount,
                toTicketResponse(nextWaitingTicket, serviceById, windowById)
        );
    }

    private OperatorWindowBoardResponse toOperatorWindowResponse(
            OperatorWindowEntity operatorWindow,
            BranchBoardTicketRow currentTicket,
            Map<UUID, QueueServiceEntity> serviceById,
            Map<UUID, OperatorWindowEntity> windowById
    ) {
        return new OperatorWindowBoardResponse(
                operatorWindow.getId(),
                operatorWindow.getNumber(),
                operatorWindow.getName(),
                operatorWindow.getStatus(),
                toTicketResponse(currentTicket, serviceById, windowById)
        );
    }

    private TicketBoardResponse toTicketResponse(
            BranchBoardTicketRow ticket,
            Map<UUID, QueueServiceEntity> serviceById,
            Map<UUID, OperatorWindowEntity> windowById
    ) {
        if (ticket == null) {
            return null;
        }

        QueueServiceEntity queueService = serviceById.get(ticket.serviceId());
        OperatorWindowEntity operatorWindow = ticket.operatorWindowId() == null
                ? null
                : windowById.get(ticket.operatorWindowId());

        return new TicketBoardResponse(
                ticket.id(),
                ticket.serviceId(),
                queueService == null ? null : queueService.getCode(),
                ticket.operatorWindowId(),
                operatorWindow == null ? null : operatorWindow.getNumber(),
                ticket.ticketNumber(),
                ticket.status(),
                ticket.priority(),
                ticket.createdAt(),
                ticket.calledAt(),
                ticket.serviceStartedAt()
        );
    }
}
