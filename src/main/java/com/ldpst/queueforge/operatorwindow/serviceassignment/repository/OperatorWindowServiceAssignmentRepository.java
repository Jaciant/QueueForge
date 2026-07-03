package com.ldpst.queueforge.operatorwindow.serviceassignment.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ldpst.queueforge.operatorwindow.serviceassignment.entity.OperatorWindowServiceAssignmentEntity;

public interface OperatorWindowServiceAssignmentRepository
        extends JpaRepository<OperatorWindowServiceAssignmentEntity, UUID> {
    boolean existsByOperatorWindowIdAndServiceId(UUID operatorWindowId, UUID serviceId);

    List<OperatorWindowServiceAssignmentEntity> findAllByOperatorWindowIdOrderByCreatedAtAsc(UUID operatorWindowId);

    @Query("""
            select assignment.serviceId
            from OperatorWindowServiceAssignmentEntity assignment
            where assignment.operatorWindowId = :operatorWindowId
            order by assignment.createdAt asc
            """)
    List<UUID> findServiceIdsByOperatorWindowId(@Param("operatorWindowId") UUID operatorWindowId);

    @Modifying
    @Query("""
            delete from OperatorWindowServiceAssignmentEntity assignment
            where assignment.operatorWindowId = :operatorWindowId
            """)
    void deleteAllByOperatorWindowId(@Param("operatorWindowId") UUID operatorWindowId);
}
