package com.demo.taskmanager.controller;

import com.demo.taskmanager.dto.CreateTaskRequest;
import com.demo.taskmanager.dto.ErrorResponse;
import com.demo.taskmanager.dto.PagedTaskResponse;
import com.demo.taskmanager.dto.TaskResponse;
import com.demo.taskmanager.dto.UpdateTaskRequest;
import com.demo.taskmanager.exception.InvalidRequestException;
import com.demo.taskmanager.model.TaskPriority;
import com.demo.taskmanager.model.TaskSortField;
import com.demo.taskmanager.model.TaskStatus;
import com.demo.taskmanager.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/**
 * REST API for task CRUD and listing operations.
 */
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@Validated
@Tag(name = "Tasks", description = "CRUD operations for tasks")
public class TaskController {

    private final TaskService taskService;

    /**
     * Creates a new task.
     *
     * @param request validated create payload
     * @return 201 Created with the persisted task and a {@code Location} header
     */
    @PostMapping
    @Operation(summary = "Create a task")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Task created",
                    content = @Content(schema = @Schema(implementation = TaskResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody CreateTaskRequest request) {
        TaskResponse created = taskService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    /**
     * Fetches a single task by id.
     *
     * @param id the task id
     * @return 200 OK with the task representation
     */
    @GetMapping("/{id}")
    @Operation(summary = "Fetch a task by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task found",
                    content = @Content(schema = @Schema(implementation = TaskResponse.class))),
            @ApiResponse(responseCode = "400", description = "Malformed id",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Task not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public TaskResponse findById(@PathVariable UUID id) {
        return taskService.findById(id);
    }

    /**
     * Replaces a task's mutable fields. The body must supply
     * {@code title}, {@code status}, and {@code priority}; PUT is a full
     * replace, not a partial update.
     *
     * @param id      the task id
     * @param request validated update payload
     * @return 200 OK with the updated task
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update a task")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task updated",
                    content = @Content(schema = @Schema(implementation = TaskResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Task not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public TaskResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateTaskRequest request) {
        return taskService.update(id, request);
    }

    /**
     * Deletes a task by id.
     *
     * @param id the task id
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a task")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Task deleted"),
            @ApiResponse(responseCode = "404", description = "Task not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        taskService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Lists tasks with pagination and optional filtering by status and/or priority.
     *
     * @param page     zero-based page index (default {@code 0}, must be {@code >= 0})
     * @param size     page size (default {@code 20}, must be {@code >= 1}; values above 100 are clamped to 100)
     * @param sort     Spring Data sort spec like {@code field,asc}; defaults to {@code createdAt,desc}.
     *                 The sort field must be one of {@link TaskSortField#allowedProperties()}.
     * @param status   optional status filter
     * @param priority optional priority filter
     * @return 200 OK with the page envelope
     */
    @GetMapping
    @Operation(summary = "List tasks with pagination and optional filtering")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page returned",
                    content = @Content(schema = @Schema(implementation = PagedTaskResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid query parameter",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public PagedTaskResponse list(
            @Parameter(description = "Zero-based page index")
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "must be >= 0") int page,
            @Parameter(description = "Page size; values above 100 are clamped to 100")
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "must be >= 1") int size,
            @Parameter(description = "Sort spec, e.g. createdAt,desc")
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @Parameter(description = "Filter by status")
            @RequestParam(required = false) TaskStatus status,
            @Parameter(description = "Filter by priority")
            @RequestParam(required = false) TaskPriority priority
    ) {
        Sort parsedSort = parseSort(sort);
        return taskService.list(page, size, parsedSort, status, priority);
    }

    private Sort parseSort(String sortSpec) {
        if (sortSpec == null || sortSpec.isBlank()) {
            return Sort.by(Sort.Direction.DESC, TaskSortField.CREATED_AT.getProperty());
        }
        String[] parts = sortSpec.split(",");
        String property = parts[0].trim();
        TaskSortField field = TaskSortField.fromProperty(property)
                .orElseThrow(() -> new InvalidRequestException("sort",
                        "sort: must be one of [" + TaskSortField.allowedProperties() + "]"));
        Sort.Direction direction;
        if (parts.length > 1) {
            String rawDir = parts[1].trim();
            direction = Sort.Direction.fromOptionalString(rawDir)
                    .orElseThrow(() -> new InvalidRequestException("sort",
                            "sort: direction must be 'asc' or 'desc'"));
        } else {
            direction = Sort.Direction.DESC;
        }
        return Sort.by(direction, field.getProperty());
    }
}
