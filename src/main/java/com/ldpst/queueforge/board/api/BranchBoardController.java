package com.ldpst.queueforge.board.api;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ldpst.queueforge.board.dto.BranchBoardResponse;
import com.ldpst.queueforge.board.service.BranchBoardService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Branch Board", description = "Read-only branch queue board API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class BranchBoardController {
    private final BranchBoardService branchBoardService;

    @Operation(
            summary = "Get branch board",
            description = "Returns the current read-only queue state for a branch: services, waiting tickets, operator windows, current tickets and active tickets."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Branch board returned"),
            @ApiResponse(responseCode = "404", description = "Branch not found")
    })
    @GetMapping("/branches/{branchId}/board")
    public BranchBoardResponse getBoard(
            @Parameter(description = "Branch identifier") @PathVariable UUID branchId
    ) {
        return branchBoardService.getBoard(branchId);
    }
}
