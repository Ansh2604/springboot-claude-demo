package com.demo.taskmanager.service;

import com.demo.taskmanager.dto.CreateTaskRequest;
import com.demo.taskmanager.dto.PagedTaskResponse;
import com.demo.taskmanager.dto.TaskResponse;
import com.demo.taskmanager.dto.UpdateTaskRequest;
import com.demo.taskmanager.exception.TaskNotFoundException;
import com.demo.taskmanager.model.Task;
import com.demo.taskmanager.model.TaskPriority;
import com.demo.taskmanager.model.TaskSortField;
import com.demo.taskmanager.model.TaskStatus;
import com.demo.taskmanager.repository.TaskRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Application service exposing CRUD and listing operations on {@link Task}.
 */
@Service
@RequiredArgsConstructor
public class TaskService {

    /** Maximum allowed page size; the controller-supplied value is clamped to this. */
    public static final int MAX_PAGE_SIZE = 100;

    private final TaskRepository taskRepository;

    /**
     * Persists a new task, applying default status/priority when omitted.
     *
     * @param request the validated create payload
     * @return the persisted task representation
     */
    @Transactional
    public TaskResponse create(CreateTaskRequest request) {
        Task task = Task.builder()
                .title(request.title().trim())
                .description(request.description())
                .status(request.status() != null ? request.status() : TaskStatus.TODO)
                .priority(request.priority() != null ? request.priority() : TaskPriority.MEDIUM)
                .dueDate(request.dueDate())
                .build();
        return TaskResponse.from(taskRepository.save(task));
    }

    /**
     * Loads a task by id.
     *
     * @param id the task id
     * @return the task representation
     * @throws TaskNotFoundException if no task exists with the given id
     */
    @Transactional(readOnly = true)
    public TaskResponse findById(UUID id) {
        return TaskResponse.from(loadOrThrow(id));
    }

    /**
     * Replaces a task's mutable fields. The request is treated as a full
     * replacement: all of {@code title}, {@code status}, and {@code priority}
     * must be supplied (enforced at the DTO layer); {@code description} and
     * {@code dueDate} are nullable and replaced as supplied. {@code id},
     * {@code createdAt}, {@code updatedAt} are server-managed and not touched.
     *
     * @param id      the task id to update
     * @param request the validated update payload
     * @return the updated task representation
     * @throws TaskNotFoundException if no task exists with the given id
     */
    @Transactional
    public TaskResponse update(UUID id, UpdateTaskRequest request) {
        Task task = loadOrThrow(id);
        task.applyUpdate(
                request.title().trim(),
                request.description(),
                request.status(),
                request.priority(),
                request.dueDate()
        );
        return TaskResponse.from(taskRepository.save(task));
    }

    /**
     * Hard-deletes a task by id.
     *
     * @param id the task id
     * @throws TaskNotFoundException if no task exists with the given id
     */
    @Transactional
    public void delete(UUID id) {
        Task task = loadOrThrow(id);
        taskRepository.delete(task);
    }

    /**
     * Lists tasks with pagination and optional filters on status and priority.
     * {@code size} is clamped to {@value #MAX_PAGE_SIZE}; a {@code null} sort
     * falls back to {@code createdAt} descending.
     *
     * @param page     zero-based page index (assumed &gt;= 0)
     * @param size     page size (assumed &gt;= 1); clamped to {@value #MAX_PAGE_SIZE}
     * @param sort     Spring Data sort spec; may be {@code null}
     * @param status   optional status filter
     * @param priority optional priority filter
     * @return the page envelope
     */
    @Transactional(readOnly = true)
    public PagedTaskResponse list(int page, int size, Sort sort, TaskStatus status, TaskPriority priority) {
        int safeSize = Math.min(size, MAX_PAGE_SIZE);
        Sort effectiveSort = sort != null ? sort : Sort.by(Sort.Direction.DESC, TaskSortField.CREATED_AT.getProperty());
        Pageable pageable = PageRequest.of(page, safeSize, effectiveSort);

        Specification<Task> spec = buildFilterSpec(status, priority);
        Page<TaskResponse> resultPage = taskRepository.findAll(spec, pageable).map(TaskResponse::from);
        return PagedTaskResponse.from(resultPage);
    }

    private Task loadOrThrow(UUID id) {
        return taskRepository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
    }

    private Specification<Task> buildFilterSpec(TaskStatus status, TaskPriority priority) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get(TaskSortField.STATUS.getProperty()), status));
            }
            if (priority != null) {
                predicates.add(cb.equal(root.get(TaskSortField.PRIORITY.getProperty()), priority));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
