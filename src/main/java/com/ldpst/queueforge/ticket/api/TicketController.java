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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Tickets", description = "Ticket issuing, waiting list, lifecycle and history API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class TicketController {
    private final TicketManagementService ticketManagementService;
    private final TicketLifecycleService ticketLifecycleService;

    @Operation(
            summary = "Issue a new ticket",
            description = "Creates a new WAITING ticket for the selected branch and queue service. Ticket numbers are generated atomically per branch, service and business date."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ticket successfully issued"),
            @ApiResponse(responseCode = "400", description = "Invalid request, disabled service or service belongs to another branch"),
            @ApiResponse(responseCode = "404", description = "Branch or queue service not found"),
            @ApiResponse(responseCode = "409", description = "Ticket number conflict")
    })
    @PostMapping("/tickets")
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse create(@Valid @RequestBody CreateTicketRequest request) {
        return ticketManagementService.create(request);
    }

    @Operation(summary = "Get ticket by id", description = "Returns ticket details by identifier.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ticket returned"),
            @ApiResponse(responseCode = "404", description = "Ticket not found")
    })
    @GetMapping("/tickets/{ticketId}")
    public TicketResponse getById(
            @Parameter(description = "Ticket identifier") @PathVariable UUID ticketId
    ) {
        return ticketManagementService.getById(ticketId);
    }

    @Operation(
            summary = "List waiting tickets in a branch",
            description = "Returns WAITING tickets for the selected branch. The list can be filtered by queue service."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Waiting tickets returned"),
            @ApiResponse(responseCode = "404", description = "Branch or queue service not found")
    })
    @GetMapping("/branches/{branchId}/tickets/waiting")
    public List<TicketResponse> getWaitingByBranch(
            @Parameter(description = "Branch identifier") @PathVariable UUID branchId,
            @Parameter(description = "Optional queue service identifier") @RequestParam(required = false) UUID serviceId
    ) {
        return ticketManagementService.getWaitingByBranch(branchId, serviceId);
    }

    @Operation(
            summary = "Start ticket service",
            description = "Moves a ticket from CALLED to IN_SERVICE and records the status history entry."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ticket service started"),
            @ApiResponse(responseCode = "400", description = "Invalid status transition"),
            @ApiResponse(responseCode = "404", description = "Ticket not found")
    })
    @PatchMapping("/tickets/{ticketId}/start-service")
    public TicketResponse startService(
            @Parameter(description = "Ticket identifier") @PathVariable UUID ticketId,
            @Valid @RequestBody(required = false) TicketActionRequest request
    ) {
        return ticketLifecycleService.startService(ticketId, reasonFrom(request));
    }

    @Operation(
            summary = "Complete ticket service",
            description = "Moves a ticket from IN_SERVICE to COMPLETED and frees the operator window for the next ticket."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ticket completed"),
            @ApiResponse(responseCode = "400", description = "Invalid status transition"),
            @ApiResponse(responseCode = "404", description = "Ticket not found")
    })
    @PatchMapping("/tickets/{ticketId}/complete")
    public TicketResponse complete(
            @Parameter(description = "Ticket identifier") @PathVariable UUID ticketId,
            @Valid @RequestBody(required = false) TicketActionRequest request
    ) {
        return ticketLifecycleService.complete(ticketId, reasonFrom(request));
    }

    @Operation(
            summary = "Skip a called ticket",
            description = "Moves a ticket from CALLED to SKIPPED when the customer does not show up."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ticket skipped"),
            @ApiResponse(responseCode = "400", description = "Invalid status transition"),
            @ApiResponse(responseCode = "404", description = "Ticket not found")
    })
    @PatchMapping("/tickets/{ticketId}/skip")
    public TicketResponse skip(
            @Parameter(description = "Ticket identifier") @PathVariable UUID ticketId,
            @Valid @RequestBody(required = false) TicketActionRequest request
    ) {
        return ticketLifecycleService.skip(ticketId, reasonFrom(request));
    }

    @Operation(
            summary = "Cancel a ticket",
            description = "Cancels a WAITING or CALLED ticket and records the status history entry."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ticket cancelled"),
            @ApiResponse(responseCode = "400", description = "Invalid status transition"),
            @ApiResponse(responseCode = "404", description = "Ticket not found")
    })
    @PatchMapping("/tickets/{ticketId}/cancel")
    public TicketResponse cancel(
            @Parameter(description = "Ticket identifier") @PathVariable UUID ticketId,
            @Valid @RequestBody(required = false) TicketActionRequest request
    ) {
        return ticketLifecycleService.cancel(ticketId, reasonFrom(request));
    }

    @Operation(
            summary = "Get ticket status history",
            description = "Returns chronological status transition history for the selected ticket."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ticket history returned"),
            @ApiResponse(responseCode = "404", description = "Ticket not found")
    })
    @GetMapping("/tickets/{ticketId}/history")
    public List<TicketStatusHistoryResponse> getHistory(
            @Parameter(description = "Ticket identifier") @PathVariable UUID ticketId
    ) {
        return ticketLifecycleService.getHistory(ticketId);
    }

    private String reasonFrom(TicketActionRequest request) {
        return request == null ? null : request.reason();
    }
}
