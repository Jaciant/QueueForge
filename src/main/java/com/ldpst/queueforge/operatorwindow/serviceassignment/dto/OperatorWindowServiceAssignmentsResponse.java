package com.ldpst.queueforge.operatorwindow.serviceassignment.dto;

import java.util.List;
import java.util.UUID;

import com.ldpst.queueforge.queueservice.dto.QueueServiceResponse;

public record OperatorWindowServiceAssignmentsResponse(
    UUID windowId,
    List<QueueServiceResponse> services
) {
}
