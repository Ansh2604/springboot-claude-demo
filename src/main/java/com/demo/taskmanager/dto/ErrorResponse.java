package com.demo.taskmanager.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * Standard error envelope returned by the global exception handler.
 */
@Schema(description = "Error response envelope")
public record ErrorResponse(
        String code,
        String message,
        List<FieldError> fieldErrors,
        Instant timestamp
) {
    /**
     * Convenience constructor for errors without per-field detail.
     *
     * @param code    short machine-readable error code
     * @param message human-readable summary
     * @return populated envelope
     */
    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, List.of(), Instant.now());
    }

    /**
     * Convenience constructor for errors with field-level detail.
     *
     * @param code        short machine-readable error code
     * @param message     human-readable summary
     * @param fieldErrors per-field error details
     * @return populated envelope
     */
    public static ErrorResponse of(String code, String message, List<FieldError> fieldErrors) {
        return new ErrorResponse(code, message, fieldErrors, Instant.now());
    }

    /**
     * Per-field validation error.
     */
    @Schema(description = "Validation error for a single field")
    public record FieldError(String field, String message) {
    }
}
