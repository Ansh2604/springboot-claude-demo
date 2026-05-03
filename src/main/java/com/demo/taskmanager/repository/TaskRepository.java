package com.demo.taskmanager.repository;

import com.demo.taskmanager.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Spring Data repository for {@link Task}. {@link JpaSpecificationExecutor} is used
 * so the service layer can compose dynamic filters on status and priority.
 */
@Repository
public interface TaskRepository extends JpaRepository<Task, UUID>, JpaSpecificationExecutor<Task> {
}
