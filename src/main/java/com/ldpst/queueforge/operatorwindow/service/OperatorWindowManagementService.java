package com.ldpst.queueforge.operatorwindow.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ldpst.queueforge.board.cache.BranchBoardCacheService;
import com.ldpst.queueforge.branch.repository.BranchRepository;
import com.ldpst.queueforge.common.exception.ConflictException;
import com.ldpst.queueforge.common.exception.NotFoundException;
import com.ldpst.queueforge.operatorwindow.dto.CreateOperatorWindowRequest;
import com.ldpst.queueforge.operatorwindow.dto.OperatorWindowResponse;
import com.ldpst.queueforge.operatorwindow.entity.OperatorWindowEntity;
import com.ldpst.queueforge.operatorwindow.entity.OperatorWindowStatus;
import com.ldpst.queueforge.operatorwindow.repository.OperatorWindowRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OperatorWindowManagementService {
    private final OperatorWindowRepository operatorWindowRepository;
    private final BranchRepository branchRepository;
    private final BranchBoardCacheService branchBoardCacheService;

    @Transactional
    public OperatorWindowResponse create(UUID branchId, CreateOperatorWindowRequest request) {
        if (!branchRepository.existsById(branchId)) {
            throw new NotFoundException("Branch not found");
        }

        if (operatorWindowRepository.existsByBranchIdAndNumber(branchId, request.number())) {
            throw new ConflictException("Operator window with this number already exists in branch");
        }

        Instant timeNow = Instant.now();

        OperatorWindowEntity operatorWindow = new OperatorWindowEntity();
        operatorWindow.setBranchId(branchId);
        operatorWindow.setNumber(request.number());
        operatorWindow.setName(normalizeName(request.name()));
        operatorWindow.setStatus(OperatorWindowStatus.CLOSED);
        operatorWindow.setCreatedAt(timeNow);
        operatorWindow.setUpdatedAt(timeNow);

        OperatorWindowEntity savedOperatorWindow = operatorWindowRepository.save(operatorWindow);
        branchBoardCacheService.evict(branchId);

        return toResponse(savedOperatorWindow);
    }

    @Transactional(readOnly = true)
    public List<OperatorWindowResponse> getByBranch(UUID branchId) {
        if (!branchRepository.existsById(branchId)) {
            throw new NotFoundException("Branch not found");
        }

        return operatorWindowRepository.findAllByBranchIdOrderByNumberAsc(branchId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OperatorWindowResponse getById(UUID windowId) {
        OperatorWindowEntity operatorWindow = findByIdOrThrow(windowId);

        return toResponse(operatorWindow);
    }

    @Transactional
    public OperatorWindowResponse open(UUID windowId) {
        return changeStatus(windowId, OperatorWindowStatus.OPEN);
    }

    @Transactional
    public OperatorWindowResponse pause(UUID windowId) {
        return changeStatus(windowId, OperatorWindowStatus.PAUSED);
    }

    @Transactional
    public OperatorWindowResponse close(UUID windowId) {
        return changeStatus(windowId, OperatorWindowStatus.CLOSED);
    }

    private OperatorWindowResponse changeStatus(UUID windowId, OperatorWindowStatus status) {
        OperatorWindowEntity operatorWindow = findByIdOrThrow(windowId);

        operatorWindow.setStatus(status);
        operatorWindow.setUpdatedAt(Instant.now());

        OperatorWindowEntity savedOperatorWindow = operatorWindowRepository.save(operatorWindow);
        branchBoardCacheService.evict(savedOperatorWindow.getBranchId());

        return toResponse(savedOperatorWindow);
    }

    private OperatorWindowEntity findByIdOrThrow(UUID windowId) {
        return operatorWindowRepository.findById(windowId)
                .orElseThrow(() -> new NotFoundException("Operator window not found"));
    }

    private String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }

        return name.trim();
    }

    private OperatorWindowResponse toResponse(OperatorWindowEntity entity) {
        return new OperatorWindowResponse(
                entity.getId(),
                entity.getBranchId(),
                entity.getNumber(),
                entity.getName(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
