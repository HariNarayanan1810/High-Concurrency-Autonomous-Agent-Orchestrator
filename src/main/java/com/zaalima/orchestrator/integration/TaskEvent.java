package com.zaalima.orchestrator.integration;

import com.zaalima.orchestrator.core.Task;
import com.zaalima.orchestrator.core.TaskStatus;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record TaskEvent(UUID eventId, int schemaVersion, UUID taskId,
                        TaskStatus status, Instant occurredAt) {

    public static final int SCHEMA_VERSION = 1;

    public TaskEvent {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(occurredAt, "occurredAt");
        if (schemaVersion != SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported task event schema version: " + schemaVersion);
        }
    }

    public static TaskEvent from(Task task) {
        return new TaskEvent(UUID.randomUUID(), SCHEMA_VERSION, task.taskId(), task.status(), task.updatedAt());
    }
}
