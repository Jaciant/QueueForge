package com.ldpst.queueforge.ticket.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tickets")
public class TicketEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, name = "branch_id")
    private UUID branchId;

    @Column(nullable = false, name = "service_id")
    private UUID serviceId;

    @Column(name = "operator_window_id")
    private UUID operatorWindowId;

    @Column(nullable = false, name = "business_date")
    private LocalDate businessDate;

    @Column(nullable = false, name = "ticket_number", length = 32)
    private String ticketNumber;

    @Column(nullable = false, name = "sequence_number")
    private Integer sequenceNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TicketStatus status;

    @Column(nullable = false)
    private Integer priority;

    @Column(nullable = false, name = "created_at")
    private Instant createdAt;

    @Column(name = "called_at")
    private Instant calledAt;

    @Column(name = "service_started_at")
    private Instant serviceStartedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "skipped_at")
    private Instant skippedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "expired_at")
    private Instant expiredAt;
}
