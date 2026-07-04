package com.ldpst.queueforge.branch.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ldpst.queueforge.branch.dto.BranchResponse;
import com.ldpst.queueforge.branch.dto.CreateBranchRequest;
import com.ldpst.queueforge.branch.service.BranchService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Branches", description = "Branch management API")
@RequestMapping("/api/v1")
@RestController
@RequiredArgsConstructor
public class BranchController {
    private final BranchService branchService;

    @Operation(
            summary = "Create a branch",
            description = "Creates a physical branch inside an organization. Tickets, services and operator windows belong to a branch."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Branch successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "404", description = "Organization not found"),
            @ApiResponse(responseCode = "409", description = "Branch with the same name already exists in this organization")
    })
    @PostMapping("/organizations/{organizationId}/branches")
    @ResponseStatus(HttpStatus.CREATED)
    public BranchResponse create(
            @Valid @RequestBody CreateBranchRequest request,
            @Parameter(description = "Organization identifier") @PathVariable UUID organizationId
    ) {
        return branchService.create(organizationId, request);
    }

    @Operation(
            summary = "List organization branches",
            description = "Returns all branches that belong to the selected organization."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Branches returned"),
            @ApiResponse(responseCode = "404", description = "Organization not found")
    })
    @GetMapping("/organizations/{organizationId}/branches")
    public List<BranchResponse> getByOrganizationId(
            @Parameter(description = "Organization identifier") @PathVariable UUID organizationId
    ) {
        return branchService.getByOrganization(organizationId);
    }

    @Operation(
            summary = "Get branch by id",
            description = "Returns branch details by identifier."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Branch returned"),
            @ApiResponse(responseCode = "404", description = "Branch not found")
    })
    @GetMapping("/branches/{branchId}")
    public BranchResponse getById(
            @Parameter(description = "Branch identifier") @PathVariable UUID branchId
    ) {
        return branchService.getById(branchId);
    }
}
