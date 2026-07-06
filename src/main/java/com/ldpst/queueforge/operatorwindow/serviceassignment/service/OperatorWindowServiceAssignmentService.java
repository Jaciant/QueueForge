package com.ldpst.queueforge.operatorwindow.serviceassignment.service;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ldpst.queueforge.board.cache.BranchBoardCacheService;
import com.ldpst.queueforge.common.exception.BadRequestException;
import com.ldpst.queueforge.common.exception.NotFoundException;
import com.ldpst.queueforge.operatorwindow.entity.OperatorWindowEntity;
import com.ldpst.queueforge.operatorwindow.repository.OperatorWindowRepository;
import com.ldpst.queueforge.operatorwindow.serviceassignment.dto.AssignOperatorWindowServicesRequest;
import com.ldpst.queueforge.operatorwindow.serviceassignment.dto.OperatorWindowServiceAssignmentsResponse;
import com.ldpst.queueforge.operatorwindow.serviceassignment.entity.OperatorWindowServiceAssignmentEntity;
import com.ldpst.queueforge.operatorwindow.serviceassignment.repository.OperatorWindowServiceAssignmentRepository;
import com.ldpst.queueforge.queueservice.dto.QueueServiceResponse;
import com.ldpst.queueforge.queueservice.entity.QueueServiceEntity;
import com.ldpst.queueforge.queueservice.repository.QueueServiceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OperatorWindowServiceAssignmentService {
    private final OperatorWindowRepository operatorWindowRepository;
    private final QueueServiceRepository queueServiceRepository;
    private final OperatorWindowServiceAssignmentRepository assignmentRepository;
    private final BranchBoardCacheService branchBoardCacheService;

    @Transactional
    public OperatorWindowServiceAssignmentsResponse replaceServices(
            UUID windowId,
            AssignOperatorWindowServicesRequest request
    ) {
        OperatorWindowEntity operatorWindow = findOperatorWindow(windowId);
        Set<UUID> requestedServiceIds = new LinkedHashSet<>(request.serviceIds());

        if (requestedServiceIds.isEmpty()) {
            assignmentRepository.deleteAllByOperatorWindowId(windowId);
            branchBoardCacheService.evict(operatorWindow.getBranchId());
            return new OperatorWindowServiceAssignmentsResponse(windowId, List.of());
        }

        List<QueueServiceEntity> queueServices = queueServiceRepository.findAllById(requestedServiceIds);
        validateAllServicesFound(requestedServiceIds, queueServices);
        validateServicesBelongToBranch(operatorWindow.getBranchId(), queueServices);

        assignmentRepository.deleteAllByOperatorWindowId(windowId);

        Instant now = Instant.now();
        List<OperatorWindowServiceAssignmentEntity> assignments = requestedServiceIds.stream()
                .map(serviceId -> createAssignment(windowId, serviceId, now))
                .toList();

        assignmentRepository.saveAll(assignments);
        branchBoardCacheService.evict(operatorWindow.getBranchId());

        return toResponse(windowId, sortByRequestOrder(requestedServiceIds, queueServices));
    }

    @Transactional(readOnly = true)
    public OperatorWindowServiceAssignmentsResponse getServices(UUID windowId) {
        findOperatorWindow(windowId);

        List<UUID> assignedServiceIds = assignmentRepository.findServiceIdsByOperatorWindowId(windowId);
        if (assignedServiceIds.isEmpty()) {
            return new OperatorWindowServiceAssignmentsResponse(windowId, List.of());
        }

        List<QueueServiceEntity> queueServices = queueServiceRepository.findAllById(assignedServiceIds);
        return toResponse(windowId, sortByRequestOrder(new LinkedHashSet<>(assignedServiceIds), queueServices));
    }

    private OperatorWindowEntity findOperatorWindow(UUID windowId) {
        return operatorWindowRepository.findById(windowId)
                .orElseThrow(() -> new NotFoundException("Operator window not found"));
    }

    private void validateAllServicesFound(Set<UUID> requestedServiceIds, List<QueueServiceEntity> queueServices) {
        Set<UUID> foundServiceIds = queueServices.stream()
                .map(QueueServiceEntity::getId)
                .collect(Collectors.toSet());

        if (!foundServiceIds.containsAll(requestedServiceIds)) {
            throw new NotFoundException("Some services were not found");
        }
    }

    private void validateServicesBelongToBranch(UUID branchId, List<QueueServiceEntity> queueServices) {
        boolean hasServiceFromAnotherBranch = queueServices.stream()
                .anyMatch(queueService -> !queueService.getBranchId().equals(branchId));

        if (hasServiceFromAnotherBranch) {
            throw new BadRequestException("All services must belong to operator window branch");
        }
    }

    private OperatorWindowServiceAssignmentEntity createAssignment(UUID windowId, UUID serviceId, Instant createdAt) {
        OperatorWindowServiceAssignmentEntity assignment = new OperatorWindowServiceAssignmentEntity();
        assignment.setOperatorWindowId(windowId);
        assignment.setServiceId(serviceId);
        assignment.setCreatedAt(createdAt);
        return assignment;
    }

    private List<QueueServiceEntity> sortByRequestOrder(Set<UUID> requestedServiceIds, List<QueueServiceEntity> queueServices) {
        List<UUID> orderedIds = List.copyOf(requestedServiceIds);

        return queueServices.stream()
                .sorted(Comparator.comparingInt(queueService -> orderedIds.indexOf(queueService.getId())))
                .toList();
    }

    private OperatorWindowServiceAssignmentsResponse toResponse(UUID windowId, List<QueueServiceEntity> queueServices) {
        return new OperatorWindowServiceAssignmentsResponse(
                windowId,
                queueServices.stream()
                        .map(this::toQueueServiceResponse)
                        .toList()
        );
    }

    private QueueServiceResponse toQueueServiceResponse(QueueServiceEntity entity) {
        return new QueueServiceResponse(
                entity.getId(),
                entity.getBranchId(),
                entity.getCode(),
                entity.getName(),
                entity.getDescription(),
                entity.getAvgServiceTimeMinutes(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
