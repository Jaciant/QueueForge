package com.ldpst.queueforge.operatorwindow.serviceassignment.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AssignOperatorWindowServicesRequest(
    @NotNull(message = "Service ids must not be null")
    @Size(max = 100, message = "Operator window can have at most 100 assigned services")
    List<@NotNull(message = "Service id must not be null") UUID> serviceIds
) {
}
