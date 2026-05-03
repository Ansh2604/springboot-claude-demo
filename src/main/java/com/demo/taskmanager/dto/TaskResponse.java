package com.demo.taskmanager.dto;

import com.demo.taskmanager.model.Task;
import com.demo.taskmanager.model.TaskPriority;
import com.demo.taskmanager.model.TaskStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Wire representation of a {@link Task}.
 */
@Schema(description = "Task representation")
public record TaskResponse(
        UUID id,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate dueDate,
        Instant createdAt,
        Instant updatedAt
) {
    /**
     * Maps a persisted {@link Task} entity to its API representation.
     *
     * @param task the entity to convert
     * @return the wire-format response
     */
    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getDueDate(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
