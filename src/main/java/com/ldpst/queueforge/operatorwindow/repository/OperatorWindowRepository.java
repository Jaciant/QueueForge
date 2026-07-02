package com.ldpst.queueforge.operatorwindow.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ldpst.queueforge.operatorwindow.entity.OperatorWindowEntity;

import jakarta.persistence.LockModeType;

public interface OperatorWindowRepository extends JpaRepository<OperatorWindowEntity, UUID> {
    boolean existsByBranchIdAndNumber(UUID branchId, Integer number);

    List<OperatorWindowEntity> findAllByBranchIdOrderByNumberAsc(UUID branchId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ow from OperatorWindowEntity ow where ow.id = :id")
    Optional<OperatorWindowEntity> findByIdForUpdate(@Param("id") UUID id);
}
