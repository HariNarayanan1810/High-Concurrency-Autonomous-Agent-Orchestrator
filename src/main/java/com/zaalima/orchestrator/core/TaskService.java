package com.zaalima.orchestrator.core;

import com.zaalima.orchestrator.config.TaskProperties;
import com.zaalima.orchestrator.config.OrchestrationProperties;
import java.time.Duration;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);
    private final ConcurrentMap<UUID, Task> tasks = new ConcurrentHashMap<>();
    private final Semaphore storageSlots;
    private final Clock clock;
    private final Duration retention;

    public TaskService(TaskProperties properties, Clock clock) {
        this(properties, clock, OrchestrationProperties.defaults());
    }

    @Autowired
    public TaskService(TaskProperties properties, Clock clock, OrchestrationProperties execution) {
        this.storageSlots = new Semaphore(properties.maxStoredTasks());
        this.clock = clock;
        this.retention = execution.retention();
    }

    public Task submit(String capability, Map<String, String> payload) {
        if (!storageSlots.tryAcquire()) {
            purgeExpired();
            if (!storageSlots.tryAcquire()) {
                throw new TaskCapacityException();
            }
        }
        try {
            Instant now = clock.instant();
            Task task;
            do {
                task = new Task(UUID.randomUUID(), capability, payload, TaskStatus.QUEUED, now, now);
            } while (tasks.putIfAbsent(task.taskId(), task) != null);
            log.info("Task accepted taskId={} status={}", task.taskId(), task.status());
            return task;
        } catch (RuntimeException exception) {
            storageSlots.release();
            throw exception;
        }
    }

    public Task findById(UUID taskId) {
        Task task = tasks.get(taskId);
        if (task == null) {
            throw new TaskNotFoundException(taskId);
        }
        return task;
    }

    public Task cancel(UUID taskId) {
        Task task = tasks.compute(taskId, (id, existing) -> {
            if (existing == null) {
                throw new TaskNotFoundException(id);
            }
            return existing.cancel(clock.instant());
        });
        log.info("Task cancellation requested taskId={} status={}", taskId, task.status());
        return task;
    }

    public boolean start(UUID taskId) {
        AtomicBoolean changed = new AtomicBoolean();
        tasks.computeIfPresent(taskId, (id, existing) -> {
            if (existing.status() != TaskStatus.QUEUED) {
                return existing;
            }
            changed.set(true);
            return existing.running(clock.instant());
        });
        if (changed.get()) {
            log.info("Task started taskId={} status=RUNNING", taskId);
        }
        return changed.get();
    }

    public boolean succeed(UUID taskId, Map<String, Map<String, String>> results) {
        AtomicBoolean changed = new AtomicBoolean();
        tasks.computeIfPresent(taskId, (id, existing) -> {
            if (existing.status() != TaskStatus.RUNNING) {
                return existing;
            }
            changed.set(true);
            return existing.succeeded(results, clock.instant());
        });
        if (changed.get()) {
            log.info("Task completed taskId={} status=SUCCEEDED agentCount={}", taskId, results.size());
        }
        return changed.get();
    }

    public boolean fail(UUID taskId, TaskFailure failure) {
        AtomicBoolean changed = new AtomicBoolean();
        tasks.computeIfPresent(taskId, (id, existing) -> {
            if (existing.status().isTerminal()) {
                return existing;
            }
            changed.set(true);
            return existing.failed(failure, clock.instant());
        });
        return changed.get();
    }

    // Reclaim only expired terminal snapshots, never queued or running work.
    public int purgeExpired() {
        Instant cutoff = clock.instant().minus(retention);
        int removed = 0;
        for (var entry : tasks.entrySet()) {
            Task task = entry.getValue();
            if (task.status().isTerminal() && !task.updatedAt().isAfter(cutoff)
                    && tasks.remove(entry.getKey(), task)) {
                storageSlots.release();
                removed++;
            }
        }
        return removed;
    }
}
