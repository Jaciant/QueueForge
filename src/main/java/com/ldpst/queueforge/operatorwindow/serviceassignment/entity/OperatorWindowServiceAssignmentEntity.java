package com.ldpst.queueforge.operatorwindow.serviceassignment.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "operator_window_services",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_operator_window_services_pair",
                columnNames = {"operator_window_id", "service_id"}
        )
)
public class OperatorWindowServiceAssignmentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, name = "operator_window_id")
    private UUID operatorWindowId;

    @Column(nullable = false, name = "service_id")
    private UUID serviceId;

    @Column(nullable = false, name = "created_at")
    private Instant createdAt;
}
