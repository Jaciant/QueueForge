package com.ldpst.queueforge.queueservice.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ldpst.queueforge.board.cache.BranchBoardCacheService;
import com.ldpst.queueforge.branch.repository.BranchRepository;
import com.ldpst.queueforge.common.exception.ConflictException;
import com.ldpst.queueforge.common.exception.NotFoundException;
import com.ldpst.queueforge.queueservice.dto.CreateQueueServiceRequest;
import com.ldpst.queueforge.queueservice.dto.QueueServiceResponse;
import com.ldpst.queueforge.queueservice.entity.QueueServiceEntity;
import com.ldpst.queueforge.queueservice.repository.QueueServiceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QueueServiceManagementService {
    private final QueueServiceRepository queueServiceRepository;
    private final BranchRepository branchRepository;
    private final BranchBoardCacheService branchBoardCacheService;

    @Transactional
    public QueueServiceResponse create(UUID branchId, CreateQueueServiceRequest request) {
        if (!branchRepository.existsById(branchId)) {
            throw new NotFoundException("Branch not found");
        }

        String code = normalizeCode(request.code());
        String name = request.name().trim();

        if (queueServiceRepository.existsByBranchIdAndCodeIgnoreCase(branchId, code)) {
            throw new ConflictException("Service with this code already exists in branch");
        }

        Instant now = Instant.now();

        QueueServiceEntity queueService = new QueueServiceEntity();
        queueService.setBranchId(branchId);
        queueService.setCode(code);
        queueService.setName(name);
        queueService.setDescription(normalizeNullableText(request.description()));
        queueService.setAvgServiceTimeMinutes(request.avgServiceTimeMinutes());
        queueService.setActive(true);
        queueService.setCreatedAt(now);
        queueService.setUpdatedAt(now);

        QueueServiceEntity savedQueueService = queueServiceRepository.save(queueService);
        branchBoardCacheService.evict(branchId);

        return toResponse(savedQueueService);
    }

    @Transactional(readOnly = true)
    public List<QueueServiceResponse> getByBranch(UUID branchId) {
        if (!branchRepository.existsById(branchId)) {
            throw new NotFoundException("Branch not found");
        }

        return queueServiceRepository.findAllByBranchIdOrderByCreatedAtAsc(branchId)
                .stream()
                .map(entity -> toResponse(entity))
                .toList();
    }

    @Transactional(readOnly = true)
    public QueueServiceResponse getById(UUID serviceId) {
        QueueServiceEntity queueService = findById(serviceId);

        return toResponse(queueService);
    }

    @Transactional
    public QueueServiceResponse enable(UUID serviceId) {
        QueueServiceEntity queueService = findById(serviceId);

        if (!queueService.isActive()) {
            queueService.setActive(true);
            queueService.setUpdatedAt(Instant.now());
        }

        branchBoardCacheService.evict(queueService.getBranchId());

        return toResponse(queueService);
    }

    @Transactional
    public QueueServiceResponse disable(UUID serviceId) {
        QueueServiceEntity queueService = findById(serviceId);

        if (queueService.isActive()) {
            queueService.setActive(false);
            queueService.setUpdatedAt(Instant.now());
        }

        branchBoardCacheService.evict(queueService.getBranchId());

        return toResponse(queueService);
    }

    private QueueServiceEntity findById(UUID serviceId) {
        return queueServiceRepository.findById(serviceId)
                .orElseThrow(() -> new NotFoundException("Service not found"));
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase();
    }

    private String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }

        String trimmedValue = value.trim();

        if (trimmedValue.isBlank()) {
            return null;
        }

        return trimmedValue;
    }

    private QueueServiceResponse toResponse(QueueServiceEntity entity) {
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
