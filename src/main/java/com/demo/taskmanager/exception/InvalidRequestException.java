package com.demo.taskmanager.exception;

/**
 * Thrown for controlled client-input failures that should surface as a 400
 * with a curated message (e.g. an unknown {@code sort} property). Distinct
 * from a raw {@link IllegalArgumentException} so the global handler can
 * safely return {@link #getMessage()} to the caller without leaking internal
 * exception text.
 */
public class InvalidRequestException extends RuntimeException {

    private final String field;

    /**
     * Creates the exception with a sanitized, caller-facing message.
     *
     * @param field   the request field or parameter that failed validation
     * @param message the message to return to the caller
     */
    public InvalidRequestException(String field, String message) {
        super(message);
        this.field = field;
    }

    /**
     * @return the field or parameter name associated with this failure
     */
    public String getField() {
        return field;
    }
}
