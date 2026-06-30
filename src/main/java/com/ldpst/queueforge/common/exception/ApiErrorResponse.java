package com.ldpst.queueforge.common.exception;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse (
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldErrorResponse> fieldErrors
) {
    public static ApiErrorResponse withoutFieldErrors(
            int status,
            String error,
            String message,
            String path
    ) {
        return new ApiErrorResponse(
                Instant.now(),
                status,
                error,
                message,
                path,
                List.of()
        );
    }
    
    public record FieldErrorResponse(
            String field,
            String message
    ) {
    }
}
