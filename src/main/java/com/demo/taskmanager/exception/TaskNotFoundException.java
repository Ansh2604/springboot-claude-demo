package com.demo.taskmanager.exception;

import java.util.UUID;

/**
 * Thrown when a task lookup or mutation targets an id that does not exist.
 */
public class TaskNotFoundException extends RuntimeException {

    /**
     * Creates the exception with a message that includes the missing id.
     *
     * @param id the task id that was not found
     */
    public TaskNotFoundException(UUID id) {
        super("Task with id " + id + " not found");
    }
}
