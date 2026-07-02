package com.ldpst.queueforge.ticket.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ldpst.queueforge.ticket.dto.CreateTicketRequest;
import com.ldpst.queueforge.ticket.dto.TicketActionRequest;
import com.ldpst.queueforge.ticket.dto.TicketResponse;
import com.ldpst.queueforge.ticket.dto.TicketStatusHistoryResponse;
import com.ldpst.queueforge.ticket.service.TicketLifecycleService;
import com.ldpst.queueforge.ticket.service.TicketManagementService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class TicketController {
    private final TicketManagementService ticketManagementService;
    private final TicketLifecycleService ticketLifecycleService;

    @PostMapping("/tickets")
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse create(@Valid @RequestBody CreateTicketRequest request) {
        return ticketManagementService.create(request);
    }

    @GetMapping("/tickets/{ticketId}")
    public TicketResponse getById(@PathVariable UUID ticketId) {
        return ticketManagementService.getById(ticketId);
    }

    @GetMapping("/branches/{branchId}/tickets/waiting")
    public List<TicketResponse> getWaitingByBranch(
            @PathVariable UUID branchId,
            @RequestParam(required = false) UUID serviceId
    ) {
        return ticketManagementService.getWaitingByBranch(branchId, serviceId);
    }

    @PatchMapping("/tickets/{ticketId}/start-service")
    public TicketResponse startService(
            @PathVariable UUID ticketId,
            @Valid @RequestBody(required = false) TicketActionRequest request
    ) {
        return ticketLifecycleService.startService(ticketId, reasonFrom(request));
    }

    @PatchMapping("/tickets/{ticketId}/complete")
    public TicketResponse complete(
            @PathVariable UUID ticketId,
            @Valid @RequestBody(required = false) TicketActionRequest request
    ) {
        return ticketLifecycleService.complete(ticketId, reasonFrom(request));
    }

    @PatchMapping("/tickets/{ticketId}/skip")
    public TicketResponse skip(
            @PathVariable UUID ticketId,
            @Valid @RequestBody(required = false) TicketActionRequest request
    ) {
        return ticketLifecycleService.skip(ticketId, reasonFrom(request));
    }

    @PatchMapping("/tickets/{ticketId}/cancel")
    public TicketResponse cancel(
            @PathVariable UUID ticketId,
            @Valid @RequestBody(required = false) TicketActionRequest request
    ) {
        return ticketLifecycleService.cancel(ticketId, reasonFrom(request));
    }

    @GetMapping("/tickets/{ticketId}/history")
    public List<TicketStatusHistoryResponse> getHistory(@PathVariable UUID ticketId) {
        return ticketLifecycleService.getHistory(ticketId);
    }

    private String reasonFrom(TicketActionRequest request) {
        return request == null ? null : request.reason();
    }
}
