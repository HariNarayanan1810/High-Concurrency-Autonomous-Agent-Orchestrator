package com.zaalima.orchestrator.core;

import com.zaalima.orchestrator.agent.Agent;
import com.zaalima.orchestrator.config.OrchestrationProperties;
import com.zaalima.orchestrator.integration.AgentClient;
import com.zaalima.orchestrator.integration.AgentDirectory;
import com.zaalima.orchestrator.integration.AgentExecutionResult;
import com.zaalima.orchestrator.integration.TaskExecutionEngine;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrchestrationService implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(OrchestrationService.class);
    private final TaskService tasks;
    private final OrchestrationProperties properties;
    private final ScheduledExecutorService timeouts;
    private final AgentDirectory directory;
    private final AgentClient client;
    private final TaskExecutionEngine engine;
    private final ConcurrentMap<UUID, Execution> active = new ConcurrentHashMap<>();
    private final AtomicBoolean closed = new AtomicBoolean();

    public OrchestrationService(TaskService tasks, OrchestrationProperties properties,
            ScheduledExecutorService timeouts, AgentDirectory directory, AgentClient client,
            TaskExecutionEngine engine) {
        this.tasks = tasks;
        this.properties = properties;
        this.timeouts = timeouts;
        this.directory = directory;
        this.client = client;
        this.engine = engine;
        if (properties.enabled() && (directory == null || client == null || engine == null)) {
            throw new IllegalStateException("Execution requires AgentDirectory, AgentClient and TaskExecutionEngine beans.");
        }
    }

    public Task submit(String capability, Map<String, String> payload) {
        if (closed.get()) {
            throw new TaskCapacityException();
        }
        Task task = tasks.submit(capability, payload);
        if (!properties.enabled()) {
            return task;
        }
        Execution execution = new Execution(task.taskId());
        active.put(task.taskId(), execution);
        if (closed.get()) {
            tasks.cancel(task.taskId());
            execution.stop();
            return tasks.findById(task.taskId());
        }
        try {
            execution.setTimeout(timeouts.schedule(() -> execution.fail("TIMED_OUT",
                    "Task execution exceeded its deadline.", null), properties.timeout().toMillis(), TimeUnit.MILLISECONDS));
            CompletableFuture<Void> dispatch = engine.submit(() -> {
                dispatch(execution);
                return null;
            });
            execution.track(dispatch);
            dispatch.whenComplete((ignored, error) -> {
                if (error != null && !execution.stopped.get()) {
                    execution.fail("EXECUTION_REJECTED", "Task dispatch did not complete.", error);
                }
            });
        } catch (RuntimeException exception) {
            execution.fail("EXECUTION_REJECTED", "The execution engine could not accept this task.", exception);
        }
        return tasks.findById(task.taskId());
    }

    private void dispatch(Execution execution) {
        try {
            if (execution.stopped.get() || !tasks.start(execution.taskId)) {
                execution.stop();
                return;
            }
            Task task = tasks.findById(execution.taskId);
            List<Agent> available = List.copyOf(directory.findAvailable(task.capability())).stream()
                    .map(agent -> new Agent(agent.id(), agent.name(), agent.version(),
                            Set.copyOf(agent.capabilities()), Map.copyOf(agent.metadata()),
                            agent.registeredAt(), agent.lastHeartbeat()))
                    .toList();
            if (execution.stopped.get()) {
                return;
            }
            if (available.isEmpty()) {
                execution.fail("NO_AGENTS", "No available agent supports the requested capability.", null);
                return;
            }
            if (available.size() > properties.maxAgents()) {
                execution.fail("AGENT_LIMIT_EXCEEDED", "Agent fan-out exceeds the configured task limit.", null);
                return;
            }
            Set<String> ids = new HashSet<>();
            for (Agent agent : available) {
                if (agent.id() == null || agent.id().isBlank() || agent.id().length() > 128
                        || !ids.add(agent.id()) || !agent.capabilities().contains(task.capability())) {
                    execution.fail("INVALID_AGENT_DIRECTORY", "Agent discovery returned an invalid candidate set.", null);
                    return;
                }
            }
            for (Agent agent : available) {
                if (execution.stopped.get()) {
                    return;
                }
                CompletableFuture<AgentExecutionResult> future;
                try {
                    future = engine.submit(() -> {
                        if (execution.stopped.get()) {
                            throw new CancellationException();
                        }
                        try {
                            return client.execute(agent, task);
                        } catch (InterruptedException interrupted) {
                            Thread.currentThread().interrupt();
                            throw interrupted;
                        }
                    });
                    execution.track(future);
                } catch (RuntimeException exception) {
                    execution.fail("EXECUTION_REJECTED", "The execution engine rejected agent work.", exception);
                    return;
                }
                future.whenComplete((result, error) -> {
                    if (execution.stopped.get()) {
                        return;
                    }
                    if (error != null) {
                        execution.fail("AGENT_FAILURE", "An agent could not complete the requested work.", error);
                    } else if (!validResult(result, task.taskId(), agent.id())) {
                        execution.fail("INVALID_AGENT_RESULT", "An agent returned an invalid or oversized result.", null);
                    } else {
                        int characters = result.output().entrySet().stream()
                                .mapToInt(entry -> entry.getKey().length() + entry.getValue().length()).sum();
                        if (execution.resultCharacters.addAndGet(characters) > 65_536) {
                            execution.fail("RESULT_LIMIT_EXCEEDED", "Combined agent output exceeds the task result limit.", null);
                            return;
                        }
                        execution.outputs.put(agent.id(), result.output());
                        if (execution.completed.incrementAndGet() == available.size()) {
                            tasks.succeed(task.taskId(), execution.outputs);
                            execution.stop();
                        }
                    }
                });
            }
        } catch (RuntimeException exception) {
            execution.fail("DISCOVERY_FAILURE", "Agent discovery could not complete.", exception);
        }
    }

    private boolean validResult(AgentExecutionResult result, UUID taskId, String agentId) {
        return result != null && taskId.equals(result.taskId()) && agentId.equals(result.agentId())
                && result.output().size() <= 64
                && result.output().entrySet().stream().allMatch(entry -> !entry.getKey().isBlank()
                && entry.getKey().length() <= 128 && entry.getValue().length() <= 8192);
    }

    public Task cancel(UUID taskId) {
        Task task = tasks.cancel(taskId);
        Execution execution = active.get(taskId);
        if (execution != null) {
            execution.stop();
        }
        return task;
    }

    public Task result(UUID taskId) {
        Task task = tasks.findById(taskId);
        if (!task.status().isTerminal()) {
            throw new TaskConflictException("Results are not ready while task status is " + task.status() + ".");
        }
        return task;
    }

    @Override
    public void close() {
        closed.set(true);
        active.forEach((id, execution) -> {
            try {
                tasks.cancel(id);
            } catch (TaskConflictException | TaskNotFoundException ignored) {
                // A terminal callback may have won the race with shutdown.
            }
            execution.stop();
        });
    }

    private final class Execution {
        private final UUID taskId;
        private final AtomicBoolean stopped = new AtomicBoolean();
        private final ConcurrentLinkedQueue<Future<?>> futures = new ConcurrentLinkedQueue<>();
        private final ConcurrentMap<String, Map<String, String>> outputs = new ConcurrentHashMap<>();
        private final AtomicInteger completed = new AtomicInteger();
        private final AtomicInteger resultCharacters = new AtomicInteger();
        private volatile Future<?> timeout;

        private Execution(UUID taskId) {
            this.taskId = taskId;
        }

        private void setTimeout(Future<?> future) {
            timeout = future;
            if (stopped.get()) {
                future.cancel(false);
            }
        }

        private void track(Future<?> future) {
            futures.add(future);
            if (stopped.get()) {
                future.cancel(true);
            }
        }

        private void fail(String code, String message, Throwable exception) {
            if (stopped.get()) {
                return;
            }
            if (tasks.fail(taskId, new TaskFailure(code, message))) {
                log.warn("Task failed taskId={} code={} exceptionType={}", taskId, code,
                        exception == null ? "none" : exception.getClass().getSimpleName());
            }
            stop();
        }

        private void stop() {
            if (stopped.compareAndSet(false, true)) {
                if (timeout != null) {
                    timeout.cancel(false);
                }
                futures.forEach(future -> future.cancel(true));
                active.remove(taskId, this);
            }
        }
    }
}
