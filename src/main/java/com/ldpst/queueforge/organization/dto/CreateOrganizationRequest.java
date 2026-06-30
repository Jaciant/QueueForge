package com.ldpst.queueforge.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateOrganizationRequest(
    @NotBlank(message = "Organization name must not be blank")
    @Size(max = 255, message = "Organization name must be at most 255 characters")
    String name,

    @Size(max = 2000, message = "Description must be at most 2000 characters")
    String description
) {}