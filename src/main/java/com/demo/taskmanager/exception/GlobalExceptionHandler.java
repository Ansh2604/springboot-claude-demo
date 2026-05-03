package com.demo.taskmanager.exception;

import com.demo.taskmanager.dto.ErrorResponse;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Maps domain and framework exceptions to the service's standard error envelope.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    private static final String TASK_NOT_FOUND = "TASK_NOT_FOUND";
    private static final String NOT_FOUND = "NOT_FOUND";
    private static final String METHOD_NOT_ALLOWED = "METHOD_NOT_ALLOWED";
    private static final String UNSUPPORTED_MEDIA_TYPE = "UNSUPPORTED_MEDIA_TYPE";
    private static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    /**
     * Maps {@link TaskNotFoundException} to 404.
     *
     * @param ex the thrown exception
     * @return 404 envelope
     */
    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(TaskNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(TASK_NOT_FOUND, ex.getMessage()));
    }

    /**
     * Maps controlled {@link InvalidRequestException}s thrown by the controller
     * (e.g. unknown sort property) to 400. The exception's message is
     * caller-facing and may be returned as-is.
     *
     * @param ex the invalid-request exception
     * @return 400 envelope with field-level detail
     */
    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequest(InvalidRequestException ex) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(VALIDATION_ERROR, ex.getMessage(),
                        List.of(new ErrorResponse.FieldError(ex.getField(), ex.getMessage()))));
    }

    /**
     * Maps Bean Validation failures on request bodies to 400 with field-level detail.
     *
     * @param ex the validation exception
     * @return 400 envelope
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBodyValidation(MethodArgumentNotValidException ex) {
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .toList();
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(VALIDATION_ERROR, "Validation failed", fieldErrors));
    }

    /**
     * Maps Bean Validation failures on controller method parameters
     * (e.g. {@code @Min} on a {@code @RequestParam}) to 400.
     *
     * @param ex the constraint-violation exception
     * @return 400 envelope
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        List<ErrorResponse.FieldError> fieldErrors = ex.getConstraintViolations().stream()
                .map(v -> {
                    String path = v.getPropertyPath().toString();
                    String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
                    return new ErrorResponse.FieldError(field, v.getMessage());
                })
                .toList();
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(VALIDATION_ERROR, "Validation failed", fieldErrors));
    }

    /**
     * Maps Spring 6.1's {@link HandlerMethodValidationException} (raised for
     * some parameter-validation failures in place of
     * {@link ConstraintViolationException}) to 400.
     *
     * @param ex the handler-method validation exception
     * @return 400 envelope
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleHandlerMethodValidation(HandlerMethodValidationException ex) {
        List<ErrorResponse.FieldError> fieldErrors = ex.getAllValidationResults().stream()
                .flatMap(result -> {
                    String field = result.getMethodParameter().getParameterName() != null
                            ? result.getMethodParameter().getParameterName()
                            : "argument";
                    return result.getResolvableErrors().stream()
                            .map(err -> new ErrorResponse.FieldError(field,
                                    err.getDefaultMessage() != null ? err.getDefaultMessage() : "invalid"));
                })
                .toList();
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(VALIDATION_ERROR, "Validation failed", fieldErrors));
    }

    /**
     * Maps a missing required {@code @RequestParam} to 400.
     *
     * @param ex the missing-parameter exception
     * @return 400 envelope
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        String field = ex.getParameterName();
        String message = field + ": is required";
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(VALIDATION_ERROR, message,
                        List.of(new ErrorResponse.FieldError(field, "is required"))));
    }

    /**
     * Maps malformed path/query types (e.g. invalid UUID, unknown enum value) to 400.
     *
     * @param ex the type-mismatch exception
     * @return 400 envelope
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String field = ex.getName();
        Class<?> required = ex.getRequiredType();
        String message;
        if (required != null && required.isEnum()) {
            String allowed = Arrays.stream(required.getEnumConstants())
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
            message = field + ": must be one of [" + allowed + "]";
        } else if (required == UUID.class) {
            message = field + ": must be a valid UUID";
        } else {
            message = field + ": invalid value";
        }
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(VALIDATION_ERROR, message,
                        List.of(new ErrorResponse.FieldError(field, message))));
    }

    /**
     * Maps unparseable bodies (bad JSON, invalid enum/date strings, unknown
     * properties on a strict DTO) to 400.
     *
     * @param ex the deserialization exception
     * @return 400 envelope
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex) {
        Throwable cause = ex.getCause();
        if (cause instanceof UnrecognizedPropertyException upe) {
            String field = upe.getPropertyName();
            String message = field + ": is not allowed";
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.of(VALIDATION_ERROR, message,
                            List.of(new ErrorResponse.FieldError(field, "is not allowed"))));
        }
        if (cause instanceof InvalidFormatException ife) {
            String field = pathRef(ife);
            Class<?> target = ife.getTargetType();
            String message;
            if (target != null && target.isEnum()) {
                String allowed = Arrays.stream(target.getEnumConstants())
                        .map(Object::toString)
                        .collect(Collectors.joining(", "));
                message = field + ": must be one of [" + allowed + "]";
            } else if (target == LocalDate.class) {
                message = field + ": must be ISO-8601 date (YYYY-MM-DD)";
            } else {
                message = field + ": invalid value";
            }
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.of(VALIDATION_ERROR, message,
                            List.of(new ErrorResponse.FieldError(field, message))));
        }
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(VALIDATION_ERROR, "Malformed request body"));
    }

    /**
     * Maps an unsupported HTTP method on a known path to 405.
     *
     * @param ex the method-not-supported exception
     * @return 405 envelope
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ErrorResponse.of(METHOD_NOT_ALLOWED, "Method not allowed: " + ex.getMethod()));
    }

    /**
     * Maps an unsupported request {@code Content-Type} to 415.
     *
     * @param ex the unsupported-media-type exception
     * @return 415 envelope
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ErrorResponse.of(UNSUPPORTED_MEDIA_TYPE, "Unsupported content type"));
    }

    /**
     * Maps a request to a non-existent static resource / unmapped path to 404.
     *
     * @param ex the no-resource-found exception
     * @return 404 envelope
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(NOT_FOUND, "Resource not found"));
    }

    /**
     * Defense-in-depth handler for any {@link IllegalArgumentException} that
     * escapes the controller. Returns a sanitized 400 — the original
     * message is logged but not echoed to the caller, since
     * {@code IllegalArgumentException} is also thrown by Spring Data
     * (e.g. {@code PropertyReferenceException}) and may include internal
     * schema details.
     *
     * @param ex the illegal-argument exception
     * @return 400 envelope with a generic message
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Unhandled IllegalArgumentException", ex);
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(VALIDATION_ERROR, "Invalid request"));
    }

    /**
     * Last-resort handler returning 500 for unexpected failures. The
     * exception is logged at ERROR so the cause can be diagnosed; the
     * response body intentionally does not include the message.
     *
     * @param ex the unexpected exception
     * @return 500 envelope
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(INTERNAL_ERROR, "Unexpected error"));
    }

    private ErrorResponse.FieldError toFieldError(FieldError fe) {
        return new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage());
    }

    private String pathRef(JsonMappingException ex) {
        return ex.getPath().stream()
                .map(JsonMappingException.Reference::getFieldName)
                .filter(Objects::nonNull)
                .reduce((a, b) -> a + "." + b)
                .orElse("body");
    }
}
