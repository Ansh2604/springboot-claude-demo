package com.demo.taskmanager.service;

import com.demo.taskmanager.dto.CreateTaskRequest;
import com.demo.taskmanager.dto.PagedTaskResponse;
import com.demo.taskmanager.dto.TaskResponse;
import com.demo.taskmanager.dto.UpdateTaskRequest;
import com.demo.taskmanager.exception.TaskNotFoundException;
import com.demo.taskmanager.model.Task;
import com.demo.taskmanager.model.TaskPriority;
import com.demo.taskmanager.model.TaskStatus;
import com.demo.taskmanager.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskService taskService;

    @Test
    void create_appliesDefaults_andTrimsTitle() {
        CreateTaskRequest request = new CreateTaskRequest(
                "  Write spec  ",
                "describe API",
                null,
                null,
                LocalDate.of(2026, 6, 1)
        );
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskService.create(request);

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        Task saved = captor.getValue();
        assertThat(saved.getTitle()).isEqualTo("Write spec");
        assertThat(saved.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(saved.getPriority()).isEqualTo(TaskPriority.MEDIUM);
        assertThat(saved.getDescription()).isEqualTo("describe API");
        assertThat(saved.getDueDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(response.title()).isEqualTo("Write spec");
    }

    @Test
    void create_keepsExplicitStatusAndPriority() {
        CreateTaskRequest request = new CreateTaskRequest(
                "Ship it",
                null,
                TaskStatus.IN_PROGRESS,
                TaskPriority.HIGH,
                null
        );
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        taskService.create(request);

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(captor.getValue().getPriority()).isEqualTo(TaskPriority.HIGH);
    }

    @Test
    void findById_returnsResponse_whenPresent() {
        UUID id = UUID.randomUUID();
        Task task = sampleTask(id);
        when(taskRepository.findById(id)).thenReturn(Optional.of(task));

        TaskResponse response = taskService.findById(id);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.title()).isEqualTo("Sample");
    }

    @Test
    void findById_throws_whenAbsent() {
        UUID id = UUID.randomUUID();
        when(taskRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.findById(id))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void update_replacesMutableFields() {
        UUID id = UUID.randomUUID();
        Task existing = sampleTask(id);
        when(taskRepository.findById(id)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateTaskRequest request = new UpdateTaskRequest(
                "  Updated  ",
                "new desc",
                TaskStatus.DONE,
                TaskPriority.LOW,
                LocalDate.of(2026, 12, 31)
        );

        TaskResponse response = taskService.update(id, request);

        assertThat(response.title()).isEqualTo("Updated");
        assertThat(response.description()).isEqualTo("new desc");
        assertThat(response.status()).isEqualTo(TaskStatus.DONE);
        assertThat(response.priority()).isEqualTo(TaskPriority.LOW);
        assertThat(response.dueDate()).isEqualTo(LocalDate.of(2026, 12, 31));
    }

    @Test
    void update_preservesId_andClearsNullableFields() {
        UUID id = UUID.randomUUID();
        Task existing = sampleTask(id);
        when(taskRepository.findById(id)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateTaskRequest request = new UpdateTaskRequest(
                "Updated",
                null,
                TaskStatus.IN_PROGRESS,
                TaskPriority.HIGH,
                null
        );

        TaskResponse response = taskService.update(id, request);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.description()).isNull();
        assertThat(response.dueDate()).isNull();
        assertThat(response.status()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(response.priority()).isEqualTo(TaskPriority.HIGH);
    }

    @Test
    void update_throws_whenAbsent() {
        UUID id = UUID.randomUUID();
        when(taskRepository.findById(id)).thenReturn(Optional.empty());
        UpdateTaskRequest request = new UpdateTaskRequest("x", null, TaskStatus.TODO, TaskPriority.MEDIUM, null);

        assertThatThrownBy(() -> taskService.update(id, request))
                .isInstanceOf(TaskNotFoundException.class);
        verify(taskRepository, never()).save(any());
    }

    @Test
    void delete_removes_whenPresent() {
        UUID id = UUID.randomUUID();
        Task existing = sampleTask(id);
        when(taskRepository.findById(id)).thenReturn(Optional.of(existing));

        taskService.delete(id);

        verify(taskRepository).delete(existing);
    }

    @Test
    void delete_throws_whenAbsent() {
        UUID id = UUID.randomUUID();
        when(taskRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.delete(id)).isInstanceOf(TaskNotFoundException.class);
        verify(taskRepository, never()).delete(any(Task.class));
    }

    @Test
    void list_clampsSize_andDefaultsSort() {
        Task task = sampleTask(UUID.randomUUID());
        Page<Task> page = new PageImpl<>(List.of(task), PageRequest.of(0, 100), 1);
        when(taskRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PagedTaskResponse response = taskService.list(0, 500, null, null, null);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(taskRepository).findAll(any(Specification.class), pageableCaptor.capture());
        Pageable used = pageableCaptor.getValue();
        assertThat(used.getPageSize()).isEqualTo(100);
        assertThat(used.getSort().getOrderFor("createdAt")).isNotNull();
        assertThat(used.getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(response.content()).hasSize(1);
        assertThat(response.totalElements()).isEqualTo(1);
    }

    @Test
    void list_appliesProvidedSort_andSize() {
        Page<Task> page = new PageImpl<>(List.of(), PageRequest.of(1, 5), 0);
        when(taskRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Sort sort = Sort.by(Sort.Direction.ASC, "title");
        taskService.list(1, 5, sort, TaskStatus.TODO, TaskPriority.HIGH);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(taskRepository).findAll(any(Specification.class), captor.capture());
        Pageable used = captor.getValue();
        assertThat(used.getPageNumber()).isEqualTo(1);
        assertThat(used.getPageSize()).isEqualTo(5);
        assertThat(used.getSort().getOrderFor("title").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    private Task sampleTask(UUID id) {
        Instant now = Instant.parse("2026-05-03T12:00:00Z");
        return Task.builder()
                .id(id)
                .title("Sample")
                .description("desc")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .dueDate(LocalDate.of(2026, 6, 1))
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
