package com.demo.taskmanager.controller;

import com.demo.taskmanager.dto.PagedTaskResponse;
import com.demo.taskmanager.dto.TaskResponse;
import com.demo.taskmanager.exception.GlobalExceptionHandler;
import com.demo.taskmanager.exception.TaskNotFoundException;
import com.demo.taskmanager.model.TaskPriority;
import com.demo.taskmanager.model.TaskStatus;
import com.demo.taskmanager.service.TaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
@Import(GlobalExceptionHandler.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskService taskService;

    @Test
    void create_returns201_andLocation() throws Exception {
        UUID id = UUID.randomUUID();
        TaskResponse response = sampleResponse(id);
        when(taskService.create(any())).thenReturn(response);

        Map<String, Object> body = Map.of(
                "title", "Write spec",
                "description", "describe API",
                "status", "TODO",
                "priority", "HIGH",
                "dueDate", "2026-06-01"
        );

        mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/v1/tasks/" + id)))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.title").value("Sample"));
    }

    @Test
    void create_returns400_whenTitleBlank() throws Exception {
        Map<String, Object> body = Map.of("title", "   ");

        mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("title"));
    }

    @Test
    void create_returns400_whenTitleTooLong() throws Exception {
        Map<String, Object> body = Map.of("title", "x".repeat(201));

        mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void create_returns400_whenStatusEnumInvalid() throws Exception {
        String body = """
                {"title":"x","status":"BOGUS"}
                """;

        mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void create_returns400_whenDueDateMalformed() throws Exception {
        String body = """
                {"title":"x","dueDate":"not-a-date"}
                """;

        mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("ISO-8601")));
    }

    @Test
    void create_returns400_whenUnknownPropertySupplied() throws Exception {
        String body = """
                {"title":"x","createdAt":"2026-01-01T00:00:00Z"}
                """;

        mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("createdAt"));
    }

    @Test
    void findById_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(taskService.findById(id)).thenReturn(sampleResponse(id));

        mockMvc.perform(get("/api/v1/tasks/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void findById_returns404_whenAbsent() throws Exception {
        UUID id = UUID.randomUUID();
        when(taskService.findById(id)).thenThrow(new TaskNotFoundException(id));

        mockMvc.perform(get("/api/v1/tasks/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TASK_NOT_FOUND"));
    }

    @Test
    void findById_returns400_whenIdMalformed() throws Exception {
        mockMvc.perform(get("/api/v1/tasks/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("must be a valid UUID")));
    }

    @Test
    void update_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(taskService.update(eq(id), any())).thenReturn(sampleResponse(id));

        Map<String, Object> body = Map.of(
                "title", "Updated",
                "status", "DONE",
                "priority", "LOW"
        );

        mockMvc.perform(put("/api/v1/tasks/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void update_returns404_whenAbsent() throws Exception {
        UUID id = UUID.randomUUID();
        when(taskService.update(eq(id), any())).thenThrow(new TaskNotFoundException(id));

        Map<String, Object> body = Map.of(
                "title", "Updated",
                "status", "DONE",
                "priority", "LOW"
        );

        mockMvc.perform(put("/api/v1/tasks/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_returns400_whenTitleBlank() throws Exception {
        UUID id = UUID.randomUUID();
        Map<String, Object> body = Map.of(
                "title", "",
                "status", "DONE",
                "priority", "LOW"
        );

        mockMvc.perform(put("/api/v1/tasks/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_returns400_whenStatusOmitted() throws Exception {
        UUID id = UUID.randomUUID();
        Map<String, Object> body = Map.of(
                "title", "Updated",
                "priority", "LOW"
        );

        mockMvc.perform(put("/api/v1/tasks/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void update_returns400_whenIdSuppliedInBody() throws Exception {
        UUID id = UUID.randomUUID();
        Map<String, Object> body = new HashMap<>();
        body.put("id", UUID.randomUUID().toString());
        body.put("title", "Updated");
        body.put("status", "DONE");
        body.put("priority", "LOW");

        mockMvc.perform(put("/api/v1/tasks/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("id"));
    }

    @Test
    void delete_returns204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/tasks/{id}", id))
                .andExpect(status().isNoContent());
        verify(taskService).delete(id);
    }

    @Test
    void delete_returns404_whenAbsent() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new TaskNotFoundException(id)).when(taskService).delete(id);

        mockMvc.perform(delete("/api/v1/tasks/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void list_returnsPage_withDefaults() throws Exception {
        TaskResponse response = sampleResponse(UUID.randomUUID());
        PagedTaskResponse paged = new PagedTaskResponse(List.of(response), 0, 20, 1, 1);
        when(taskService.list(eq(0), eq(20), any(Sort.class), eq(null), eq(null))).thenReturn(paged);

        mockMvc.perform(get("/api/v1/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(response.id().toString()))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void list_passesFilters_toService() throws Exception {
        when(taskService.list(anyInt(), anyInt(), any(Sort.class), eq(TaskStatus.TODO), eq(TaskPriority.HIGH)))
                .thenReturn(new PagedTaskResponse(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/v1/tasks").param("status", "TODO").param("priority", "HIGH"))
                .andExpect(status().isOk());

        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);
        verify(taskService).list(eq(0), eq(20), sortCaptor.capture(), eq(TaskStatus.TODO), eq(TaskPriority.HIGH));
        assertThat(sortCaptor.getValue().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void list_returns400_whenPageNegative() throws Exception {
        mockMvc.perform(get("/api/v1/tasks").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void list_returns400_whenSizeZero() throws Exception {
        mockMvc.perform(get("/api/v1/tasks").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void list_returns400_whenStatusEnumInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/tasks").param("status", "BOGUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("must be one of")));
    }

    @Test
    void list_returns400_whenSortPropertyUnknown() throws Exception {
        mockMvc.perform(get("/api/v1/tasks").param("sort", "passwordHash,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("must be one of")));
    }

    @Test
    void list_returns400_whenSortDirectionUnknown() throws Exception {
        mockMvc.perform(get("/api/v1/tasks").param("sort", "createdAt,sideways"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("'asc' or 'desc'")));
    }

    private TaskResponse sampleResponse(UUID id) {
        Instant now = Instant.parse("2026-05-03T12:00:00Z");
        return new TaskResponse(
                id,
                "Sample",
                "desc",
                TaskStatus.TODO,
                TaskPriority.MEDIUM,
                LocalDate.of(2026, 6, 1),
                now,
                now
        );
    }
}
