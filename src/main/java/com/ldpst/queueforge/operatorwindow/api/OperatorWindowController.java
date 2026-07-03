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
import com.ldpst.queueforge.operatorwindow.serviceassignment.dto.AssignOperatorWindowServicesRequest;
import com.ldpst.queueforge.operatorwindow.serviceassignment.dto.OperatorWindowServiceAssignmentsResponse;
import com.ldpst.queueforge.operatorwindow.serviceassignment.service.OperatorWindowServiceAssignmentService;
import com.ldpst.queueforge.operatorwindow.service.OperatorWindowManagementService;
import com.ldpst.queueforge.ticket.dto.TicketResponse;
import com.ldpst.queueforge.ticket.service.TicketCallService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class OperatorWindowController {
    private final OperatorWindowManagementService operatorWindowManagementService;
    private final OperatorWindowServiceAssignmentService operatorWindowServiceAssignmentService;
    private final TicketCallService ticketCallService;

    @PostMapping("/branches/{branchId}/operator-windows")
    @ResponseStatus(HttpStatus.CREATED)
    public OperatorWindowResponse create(
            @PathVariable UUID branchId,
            @Valid @RequestBody CreateOperatorWindowRequest request
    ) {
        return operatorWindowManagementService.create(branchId, request);
    }

    @GetMapping("/branches/{branchId}/operator-windows")
    public List<OperatorWindowResponse> getByBranch(@PathVariable UUID branchId) {
        return operatorWindowManagementService.getByBranch(branchId);
    }

    @GetMapping("/operator-windows/{windowId}")
    public OperatorWindowResponse getById(@PathVariable UUID windowId) {
        return operatorWindowManagementService.getById(windowId);
    }

    @PatchMapping("/operator-windows/{windowId}/open")
    public OperatorWindowResponse open(@PathVariable UUID windowId) {
        return operatorWindowManagementService.open(windowId);
    }

    @GetMapping("/operator-windows/{windowId}/services")
    public OperatorWindowServiceAssignmentsResponse getAssignedServices(@PathVariable UUID windowId) {
        return operatorWindowServiceAssignmentService.getServices(windowId);
    }

    @PutMapping("/operator-windows/{windowId}/services")
    public OperatorWindowServiceAssignmentsResponse replaceAssignedServices(
            @PathVariable UUID windowId,
            @Valid @RequestBody AssignOperatorWindowServicesRequest request
    ) {
        return operatorWindowServiceAssignmentService.replaceServices(windowId, request);
    }

    @PostMapping("/operator-windows/{windowId}/call-next")
    public TicketResponse callNext(
            @PathVariable UUID windowId,
            @RequestParam(required = false) UUID serviceId
    ) {
        return ticketCallService.callNext(windowId, serviceId);
    }

    @PatchMapping("/operator-windows/{windowId}/pause")
    public OperatorWindowResponse pause(@PathVariable UUID windowId) {
        return operatorWindowManagementService.pause(windowId);
    }

    @PatchMapping("/operator-windows/{windowId}/close")
    public OperatorWindowResponse close(@PathVariable UUID windowId) {
        return operatorWindowManagementService.close(windowId);
    }
}
