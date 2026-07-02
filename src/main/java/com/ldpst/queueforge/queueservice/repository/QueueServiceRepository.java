package com.ldpst.queueforge.queueservice.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ldpst.queueforge.queueservice.entity.QueueServiceEntity;

public interface QueueServiceRepository extends JpaRepository<QueueServiceEntity, UUID> {
    boolean existsByBranchIdAndCodeIgnoreCase(UUID branchId, String code);

    List<QueueServiceEntity> findAllByBranchIdOrderByCreatedAtAsc(UUID branchId);
}
