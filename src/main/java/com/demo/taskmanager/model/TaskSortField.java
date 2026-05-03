package com.demo.taskmanager.model;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Allow-list of properties on {@link Task} that may be used as a sort key on
 * the list endpoint. Used to gate the {@code sort} query parameter so JPA
 * cannot be tricked into projecting arbitrary entity fields.
 */
public enum TaskSortField {
    ID("id"),
    TITLE("title"),
    STATUS("status"),
    PRIORITY("priority"),
    DUE_DATE("dueDate"),
    CREATED_AT("createdAt"),
    UPDATED_AT("updatedAt");

    private final String property;

    TaskSortField(String property) {
        this.property = property;
    }

    /**
     * @return the JPA property name to pass to {@code Sort.by(...)}
     */
    public String getProperty() {
        return property;
    }

    /**
     * Resolves a property name (as it would appear in a {@code sort=} query
     * parameter) to its enum constant.
     *
     * @param property the JPA property name
     * @return the matching enum constant, if any
     */
    public static Optional<TaskSortField> fromProperty(String property) {
        if (property == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(v -> v.property.equals(property))
                .findFirst();
    }

    /**
     * @return comma-separated list of allowed property names, suitable for
     *         use in error messages
     */
    public static String allowedProperties() {
        return Arrays.stream(values())
                .map(TaskSortField::getProperty)
                .collect(Collectors.joining(", "));
    }
}
