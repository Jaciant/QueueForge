package com.ldpst.queueforge.organization.service;

import com.ldpst.queueforge.organization.dto.OrganizationResponse;
import com.ldpst.queueforge.organization.entity.OrganizationEntity;
import com.ldpst.queueforge.organization.entity.OrganizationStatus;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.ldpst.queueforge.organization.dto.CreateOrganizationRequest;
import com.ldpst.queueforge.organization.repository.OrganizationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrganizationService {
    private final OrganizationRepository organizationRepository;

    public OrganizationResponse create(CreateOrganizationRequest request) {
        if (organizationRepository.existsByNameIgnoreCase(request.name())) {
            throw new IllegalArgumentException("Organization with this name already exists");
        }

        Instant now = Instant.now();

        OrganizationEntity organization = new OrganizationEntity();
        organization.setName(request.name());
        organization.setDescription(request.description());

        organization.setStatus(OrganizationStatus.ACTIVE);
        organization.setCreatedAt(now);
        organization.setUpdatedAt(now);

        OrganizationEntity savedOrganization = organizationRepository.save(organization);

        return toResponse(savedOrganization);
    }

    public OrganizationResponse toResponse(OrganizationEntity organization) {
        return new OrganizationResponse(
            organization.getId(),
            organization.getName(),
            organization.getDescription(),
            organization.getStatus(),
            organization.getCreatedAt(),
            organization.getUpdatedAt()
        );
    }
}
