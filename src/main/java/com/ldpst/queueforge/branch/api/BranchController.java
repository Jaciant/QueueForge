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

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;


@RequestMapping("/api/v1")
@RestController
@RequiredArgsConstructor
public class BranchController {
    private final BranchService branchService;

    @PostMapping("/organizations/{organizationId}/branches")
    @ResponseStatus(HttpStatus.CREATED)
    public BranchResponse create(@Valid @RequestBody CreateBranchRequest request, @PathVariable UUID organizationId) {
        return branchService.create(organizationId, request);
    }

    @GetMapping("/organizations/{organizationId}/branches")
    public List<BranchResponse> getByOrganizationId(@PathVariable UUID organizationId) {
        return branchService.getByOrganization(organizationId);
    }

    @GetMapping("/branches/{branchId}")
    public BranchResponse getById(@PathVariable UUID branchId) {
        return branchService.getById(branchId);
    }
    
}
