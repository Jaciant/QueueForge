package com.ldpst.queueforge.organization.dto;

import java.time.Instant;
import java.util.UUID;

import com.ldpst.queueforge.organization.entity.OrganizationStatus;

public record OrganizationResponse (
    UUID id,
    String name,
    String description,
    OrganizationStatus status,
    Instant createdAt,
    Instant updatedAt
) {}
