package com.ldpst.queueforge.operatorwindow.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateOperatorWindowRequest(
    @NotNull(message = "Window number must not be null")
    @Min(value = 1, message = "Window number must be greater than 0")
    Integer number,

    @Size(max = 255, message = "Window name must be at most 255 characters")
    String name
) {
}
