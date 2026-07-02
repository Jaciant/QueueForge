package com.ldpst.queueforge.queueservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateQueueServiceRequest(
    @NotBlank(message = "Service code must not be blank")
    @Size(max = 32, message = "Service code must be at most 32 characters")
    String code,

    @NotBlank(message = "Service name must not be blank")
    @Size(max = 255, message = "Service name must be at most 255 characters")
    String name,

    @Size(max = 2000, message = "Description must be at most 2000 characters")
    String description,

    @Min(value = 1, message = "Average service time must be greater than 0")
    Integer avgServiceTimeMinutes
) {
}
