package com.demo.taskmanager.dto;

import com.demo.taskmanager.model.TaskPriority;
import com.demo.taskmanager.model.TaskStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request body for replacing a task's mutable fields. PUT is treated as a
 * full replace: {@code title}, {@code status}, and {@code priority} are
 * required so the caller cannot omit them and silently regress the row to
 * defaults. Unknown fields (including the server-managed {@code id},
 * {@code createdAt}, {@code updatedAt}) are rejected.
 */
@Schema(description = "Payload for updating a task")
@JsonIgnoreProperties(ignoreUnknown = false)
public record UpdateTaskRequest(
        @Schema(description = "Task title", example = "Write spec", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "must not be blank")
        @Size(max = 200, message = "length must be <= 200")
        String title,

        @Schema(description = "Free-form description")
        @Size(max = 2000, message = "length must be <= 2000")
        String description,

        @Schema(description = "Lifecycle status", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "must not be null")
        TaskStatus status,

        @Schema(description = "Priority", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "must not be null")
        TaskPriority priority,

        @Schema(description = "Due date in YYYY-MM-DD", example = "2026-06-01")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate dueDate
) {
}
