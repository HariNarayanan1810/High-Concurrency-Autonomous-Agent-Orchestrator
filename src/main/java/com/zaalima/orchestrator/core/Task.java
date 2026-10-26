package com.zaalima.orchestrator.core;

import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;
import java.util.Collections;
import java.util.UUID;

public record Task(UUID taskId, String capability, Map<String, String> payload,
                   TaskStatus status, Instant submittedAt, Instant updatedAt,
                   Map<String, Map<String, String>> results, TaskFailure failure) {

    public Task {
        payload = Map.copyOf(payload);
        Map<String, Map<String, String>> copied = new TreeMap<>();
        results.forEach((agentId, output) -> copied.put(agentId, Map.copyOf(output)));
        results = Collections.unmodifiableMap(copied);
    }

    public Task(UUID taskId, String capability, Map<String, String> payload,
                TaskStatus status, Instant submittedAt, Instant updatedAt) {
        this(taskId, capability, payload, status, submittedAt, updatedAt, Map.of(), null);
    }

    public Task running(Instant now) {
        if (status != TaskStatus.QUEUED) {
            throw new TaskConflictException("Only queued tasks can start.");
        }
        return new Task(taskId, capability, payload, TaskStatus.RUNNING, submittedAt, now);
    }

    public Task succeeded(Map<String, Map<String, String>> outputs, Instant now) {
        if (status != TaskStatus.RUNNING) {
            throw new TaskConflictException("Only running tasks can succeed.");
        }
        return new Task(taskId, capability, payload, TaskStatus.SUCCEEDED, submittedAt, now, outputs, null);
    }

    public Task failed(TaskFailure problem, Instant now) {
        if (status.isTerminal()) {
            throw new TaskConflictException("A terminal task cannot fail again.");
        }
        return new Task(taskId, capability, payload, TaskStatus.FAILED, submittedAt, now, Map.of(), problem);
    }

    public Task cancel(Instant now) {
        if (status == TaskStatus.CANCELLED) {
            return this;
        }
        if (status.isTerminal()) {
            throw new TaskConflictException("A " + status + " task cannot be cancelled.");
        }
        return new Task(taskId, capability, payload, TaskStatus.CANCELLED, submittedAt, now);
    }
}
