package com.ldpst.queueforge.organization.repository;

import com.ldpst.queueforge.organization.entity.OrganizationEntity;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<OrganizationEntity, UUID> {
    boolean existsByNameIgnoreCase(String name);
}