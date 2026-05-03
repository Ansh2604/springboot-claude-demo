package com.demo.taskmanager.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * JPA entity representing a unit of work tracked by the task-manager service.
 * Server-managed fields ({@code id}, {@code createdAt}, {@code updatedAt})
 * are populated by {@link AuditingEntityListener} and {@link #onCreate()}.
 */
@Entity
@Table(
        name = "tasks",
        indexes = {
                @Index(name = "idx_tasks_status", columnList = "status"),
                @Index(name = "idx_tasks_priority", columnList = "priority"),
                @Index(name = "idx_tasks_status_priority", columnList = "status,priority"),
                @Index(name = "idx_tasks_created_at", columnList = "created_at")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Task {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private TaskStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 8)
    private TaskPriority priority;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Populates server-managed defaults immediately before the row is inserted.
     * Auditing timestamps are populated separately by {@link AuditingEntityListener}.
     */
    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (status == null) {
            status = TaskStatus.TODO;
        }
        if (priority == null) {
            priority = TaskPriority.MEDIUM;
        }
    }

    /**
     * Replaces the entity's mutable fields. Server-managed fields
     * ({@code id}, {@code createdAt}, {@code updatedAt}) are not touched —
     * {@code updatedAt} is refreshed by {@link AuditingEntityListener} on flush.
     *
     * @param title       new title (required, non-blank)
     * @param description new description (nullable)
     * @param status      new status (required)
     * @param priority    new priority (required)
     * @param dueDate     new due date (nullable)
     */
    public void applyUpdate(String title,
                            String description,
                            TaskStatus status,
                            TaskPriority priority,
                            LocalDate dueDate) {
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.dueDate = dueDate;
    }
}
