package com.demo.taskmanager.integration;

import com.demo.taskmanager.repository.TaskRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack happy-path integration test running against the in-memory H2 store
 * configured for the {@code dev} profile (PostgreSQL compatibility mode). The
 * test exercises the controller → service → repository → JPA chain end-to-end.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class TaskApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskRepository taskRepository;

    @BeforeEach
    void wipe() {
        taskRepository.deleteAll();
    }

    @Test
    void crudGoldenPath() throws Exception {
        Map<String, Object> createBody = Map.of(
                "title", "End-to-end task",
                "description", "verifies the full stack",
                "priority", "HIGH",
                "dueDate", "2026-12-31"
        );

        MvcResult created = mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andReturn();

        JsonNode createdNode = objectMapper.readTree(created.getResponse().getContentAsString());
        UUID id = UUID.fromString(createdNode.get("id").asText());

        mockMvc.perform(get("/api/v1/tasks/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("End-to-end task"));

        Map<String, Object> updateBody = Map.of(
                "title", "Updated title",
                "status", "DONE",
                "priority", "LOW"
        );
        mockMvc.perform(put("/api/v1/tasks/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated title"))
                .andExpect(jsonPath("$.status").value("DONE"));

        mockMvc.perform(get("/api/v1/tasks").param("status", "DONE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/v1/tasks").param("status", "TODO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(delete("/api/v1/tasks/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/tasks/{id}", id))
                .andExpect(status().isNotFound());

        assertThat(taskRepository.count()).isZero();
    }

    @Test
    void list_clampsSize_to100() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("title", "task " + i))))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/api/v1/tasks").param("size", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100))
                .andExpect(jsonPath("$.totalElements").value(5));
    }
}
