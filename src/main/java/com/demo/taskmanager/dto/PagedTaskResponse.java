package com.demo.taskmanager.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Page envelope returned by the list endpoint.
 */
@Schema(description = "Paginated list of tasks")
public record PagedTaskResponse(
        List<TaskResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public PagedTaskResponse {
        content = List.copyOf(content);
    }

    /**
     * Builds a {@link PagedTaskResponse} from a Spring Data {@link Page} of tasks.
     *
     * @param page the page of mapped task responses
     * @return the wire envelope
     */
    public static PagedTaskResponse from(Page<TaskResponse> page) {
        return new PagedTaskResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
