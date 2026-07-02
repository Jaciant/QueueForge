package com.ldpst.queueforge.operatorwindow.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ldpst.queueforge.operatorwindow.entity.OperatorWindowEntity;

public interface OperatorWindowRepository extends JpaRepository<OperatorWindowEntity, UUID> {
    boolean existsByBranchIdAndNumber(UUID branchId, Integer number);

    List<OperatorWindowEntity> findAllByBranchIdOrderByNumberAsc(UUID branchId);
}
