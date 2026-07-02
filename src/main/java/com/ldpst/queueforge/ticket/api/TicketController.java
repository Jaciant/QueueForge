package com.ldpst.queueforge.ticket.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ldpst.queueforge.ticket.dto.CreateTicketRequest;
import com.ldpst.queueforge.ticket.dto.TicketResponse;
import com.ldpst.queueforge.ticket.service.TicketManagementService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class TicketController {
    private final TicketManagementService ticketManagementService;

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
}
