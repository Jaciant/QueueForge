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

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class QueueServiceController {
    private final QueueServiceManagementService queueServiceManagementService;

    @PostMapping("/branches/{branchId}/services")
    @ResponseStatus(HttpStatus.CREATED)
    public QueueServiceResponse create(
            @PathVariable UUID branchId,
            @Valid @RequestBody CreateQueueServiceRequest request
    ) {
        return queueServiceManagementService.create(branchId, request);
    }

    @GetMapping("/branches/{branchId}/services")
    public List<QueueServiceResponse> getByBranch(@PathVariable UUID branchId) {
        return queueServiceManagementService.getByBranch(branchId);
    }

    @GetMapping("/services/{serviceId}")
    public QueueServiceResponse getById(@PathVariable UUID serviceId) {
        return queueServiceManagementService.getById(serviceId);
    }

    @PatchMapping("/services/{serviceId}/enable")
    public QueueServiceResponse enable(@PathVariable UUID serviceId) {
        return queueServiceManagementService.enable(serviceId);
    }

    @PatchMapping("/services/{serviceId}/disable")
    public QueueServiceResponse disable(@PathVariable UUID serviceId) {
        return queueServiceManagementService.disable(serviceId);
    }
}
