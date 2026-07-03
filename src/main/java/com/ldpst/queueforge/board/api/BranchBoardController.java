package com.ldpst.queueforge.board.api;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ldpst.queueforge.board.dto.BranchBoardResponse;
import com.ldpst.queueforge.board.service.BranchBoardService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class BranchBoardController {
    private final BranchBoardService branchBoardService;

    @GetMapping("/branches/{branchId}/board")
    public BranchBoardResponse getBoard(@PathVariable UUID branchId) {
        return branchBoardService.getBoard(branchId);
    }
}
