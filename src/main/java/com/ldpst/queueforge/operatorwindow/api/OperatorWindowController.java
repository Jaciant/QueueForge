package com.ldpst.queueforge.operatorwindow.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ldpst.queueforge.operatorwindow.dto.CreateOperatorWindowRequest;
import com.ldpst.queueforge.operatorwindow.dto.OperatorWindowResponse;
import com.ldpst.queueforge.operatorwindow.service.OperatorWindowManagementService;
import com.ldpst.queueforge.operatorwindow.serviceassignment.dto.AssignOperatorWindowServicesRequest;
import com.ldpst.queueforge.operatorwindow.serviceassignment.dto.OperatorWindowServiceAssignmentsResponse;
import com.ldpst.queueforge.operatorwindow.serviceassignment.service.OperatorWindowServiceAssignmentService;
import com.ldpst.queueforge.ticket.dto.TicketResponse;
import com.ldpst.queueforge.ticket.service.TicketCallService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Operator Windows", description = "Operator window management, service assignments and ticket calling API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class OperatorWindowController {
    private final OperatorWindowManagementService operatorWindowManagementService;
    private final OperatorWindowServiceAssignmentService operatorWindowServiceAssignmentService;
    private final TicketCallService ticketCallService;

    @Operation(
            summary = "Create an operator window",
            description = "Creates an operator window inside a branch. New windows are created in CLOSED status."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Operator window successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "404", description = "Branch not found"),
            @ApiResponse(responseCode = "409", description = "Window with the same number already exists in this branch")
    })
    @PostMapping("/branches/{branchId}/operator-windows")
    @ResponseStatus(HttpStatus.CREATED)
    public OperatorWindowResponse create(
            @Parameter(description = "Branch identifier") @PathVariable UUID branchId,
            @Valid @RequestBody CreateOperatorWindowRequest request
    ) {
        return operatorWindowManagementService.create(branchId, request);
    }

    @Operation(
            summary = "List branch operator windows",
            description = "Returns all operator windows configured for the selected branch."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Operator windows returned"),
            @ApiResponse(responseCode = "404", description = "Branch not found")
    })
    @GetMapping("/branches/{branchId}/operator-windows")
    public List<OperatorWindowResponse> getByBranch(
            @Parameter(description = "Branch identifier") @PathVariable UUID branchId
    ) {
        return operatorWindowManagementService.getByBranch(branchId);
    }

    @Operation(summary = "Get operator window by id", description = "Returns operator window details by identifier.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Operator window returned"),
            @ApiResponse(responseCode = "404", description = "Operator window not found")
    })
    @GetMapping("/operator-windows/{windowId}")
    public OperatorWindowResponse getById(
            @Parameter(description = "Operator window identifier") @PathVariable UUID windowId
    ) {
        return operatorWindowManagementService.getById(windowId);
    }

    @Operation(
            summary = "Open an operator window",
            description = "Changes window status to OPEN. Only open windows can call tickets."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Operator window opened"),
            @ApiResponse(responseCode = "404", description = "Operator window not found")
    })
    @PatchMapping("/operator-windows/{windowId}/open")
    public OperatorWindowResponse open(
            @Parameter(description = "Operator window identifier") @PathVariable UUID windowId
    ) {
        return operatorWindowManagementService.open(windowId);
    }

    @Operation(
            summary = "Get services assigned to an operator window",
            description = "Returns queue services that can be handled by the selected operator window."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Assigned services returned"),
            @ApiResponse(responseCode = "404", description = "Operator window not found")
    })
    @GetMapping("/operator-windows/{windowId}/services")
    public OperatorWindowServiceAssignmentsResponse getAssignedServices(
            @Parameter(description = "Operator window identifier") @PathVariable UUID windowId
    ) {
        return operatorWindowServiceAssignmentService.getServices(windowId);
    }

    @Operation(
            summary = "Replace services assigned to an operator window",
            description = "Replaces the full list of queue services that can be handled by the selected operator window."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Assigned services replaced"),
            @ApiResponse(responseCode = "400", description = "Invalid request body or service belongs to another branch"),
            @ApiResponse(responseCode = "404", description = "Operator window or queue service not found")
    })
    @PutMapping("/operator-windows/{windowId}/services")
    public OperatorWindowServiceAssignmentsResponse replaceAssignedServices(
            @Parameter(description = "Operator window identifier") @PathVariable UUID windowId,
            @Valid @RequestBody AssignOperatorWindowServicesRequest request
    ) {
        return operatorWindowServiceAssignmentService.replaceServices(windowId, request);
    }

    @Operation(
            summary = "Call the next waiting ticket",
            description = "Calls the next WAITING ticket to this operator window. If serviceId is omitted, only services assigned to the window are considered."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Next ticket called"),
            @ApiResponse(responseCode = "400", description = "Requested service is not assigned to this window"),
            @ApiResponse(responseCode = "404", description = "Operator window or service not found"),
            @ApiResponse(responseCode = "409", description = "Window is not open, has no assigned services, has an active ticket or no waiting ticket exists")
    })
    @PostMapping("/operator-windows/{windowId}/call-next")
    public TicketResponse callNext(
            @Parameter(description = "Operator window identifier") @PathVariable UUID windowId,
            @Parameter(description = "Optional queue service identifier") @RequestParam(required = false) UUID serviceId
    ) {
        return ticketCallService.callNext(windowId, serviceId);
    }

    @Operation(
            summary = "Pause an operator window",
            description = "Changes window status to PAUSED. Paused windows cannot call new tickets."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Operator window paused"),
            @ApiResponse(responseCode = "404", description = "Operator window not found")
    })
    @PatchMapping("/operator-windows/{windowId}/pause")
    public OperatorWindowResponse pause(
            @Parameter(description = "Operator window identifier") @PathVariable UUID windowId
    ) {
        return operatorWindowManagementService.pause(windowId);
    }

    @Operation(
            summary = "Close an operator window",
            description = "Changes window status to CLOSED. Closed windows cannot call new tickets."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Operator window closed"),
            @ApiResponse(responseCode = "404", description = "Operator window not found")
    })
    @PatchMapping("/operator-windows/{windowId}/close")
    public OperatorWindowResponse close(
            @Parameter(description = "Operator window identifier") @PathVariable UUID windowId
    ) {
        return operatorWindowManagementService.close(windowId);
    }
}
