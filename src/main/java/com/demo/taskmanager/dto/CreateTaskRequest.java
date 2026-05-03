package com.demo.taskmanager.dto;

import com.demo.taskmanager.model.TaskPriority;
import com.demo.taskmanager.model.TaskStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request body for creating a new task. Unknown fields (including the
 * server-managed {@code id}, {@code createdAt}, {@code updatedAt}) are
 * rejected so callers cannot mass-assign protected attributes.
 */
@Schema(description = "Payload for creating a task")
@JsonIgnoreProperties(ignoreUnknown = false)
public record CreateTaskRequest(
        @Schema(description = "Task title", example = "Write spec", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "must not be blank")
        @Size(max = 200, message = "length must be <= 200")
        String title,

        @Schema(description = "Free-form description", example = "Document the API contract")
        @Size(max = 2000, message = "length must be <= 2000")
        String description,

        @Schema(description = "Lifecycle status; defaults to TODO when omitted")
        TaskStatus status,

        @Schema(description = "Priority; defaults to MEDIUM when omitted")
        TaskPriority priority,

        @Schema(description = "Due date in YYYY-MM-DD", example = "2026-06-01")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate dueDate
) {
}
