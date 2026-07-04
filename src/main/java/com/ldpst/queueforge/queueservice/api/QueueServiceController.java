package com.ldpst.queueforge.queueservice.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ldpst.queueforge.queueservice.dto.CreateQueueServiceRequest;
import com.ldpst.queueforge.queueservice.dto.QueueServiceResponse;
import com.ldpst.queueforge.queueservice.service.QueueServiceManagementService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Queue Services", description = "Service catalog API for branch queues")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class QueueServiceController {
    private final QueueServiceManagementService queueServiceManagementService;

    @Operation(
            summary = "Create a queue service",
            description = "Creates a service category inside a branch, for example PASSPORT, TAX or CONSULTATION."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Queue service successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "404", description = "Branch not found"),
            @ApiResponse(responseCode = "409", description = "Queue service with the same code already exists in this branch")
    })
    @PostMapping("/branches/{branchId}/services")
    @ResponseStatus(HttpStatus.CREATED)
    public QueueServiceResponse create(
            @Parameter(description = "Branch identifier") @PathVariable UUID branchId,
            @Valid @RequestBody CreateQueueServiceRequest request
    ) {
        return queueServiceManagementService.create(branchId, request);
    }

    @Operation(
            summary = "List branch services",
            description = "Returns all queue services configured for the selected branch."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Queue services returned"),
            @ApiResponse(responseCode = "404", description = "Branch not found")
    })
    @GetMapping("/branches/{branchId}/services")
    public List<QueueServiceResponse> getByBranch(
            @Parameter(description = "Branch identifier") @PathVariable UUID branchId
    ) {
        return queueServiceManagementService.getByBranch(branchId);
    }

    @Operation(
            summary = "Get queue service by id",
            description = "Returns service details by identifier."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Queue service returned"),
            @ApiResponse(responseCode = "404", description = "Queue service not found")
    })
    @GetMapping("/services/{serviceId}")
    public QueueServiceResponse getById(
            @Parameter(description = "Queue service identifier") @PathVariable UUID serviceId
    ) {
        return queueServiceManagementService.getById(serviceId);
    }

    @Operation(
            summary = "Enable a queue service",
            description = "Marks a queue service as active so new tickets can be issued for it."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Queue service enabled"),
            @ApiResponse(responseCode = "404", description = "Queue service not found")
    })
    @PatchMapping("/services/{serviceId}/enable")
    public QueueServiceResponse enable(
            @Parameter(description = "Queue service identifier") @PathVariable UUID serviceId
    ) {
        return queueServiceManagementService.enable(serviceId);
    }

    @Operation(
            summary = "Disable a queue service",
            description = "Marks a queue service as inactive. New tickets cannot be issued for disabled services."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Queue service disabled"),
            @ApiResponse(responseCode = "404", description = "Queue service not found")
    })
    @PatchMapping("/services/{serviceId}/disable")
    public QueueServiceResponse disable(
            @Parameter(description = "Queue service identifier") @PathVariable UUID serviceId
    ) {
        return queueServiceManagementService.disable(serviceId);
    }
}
