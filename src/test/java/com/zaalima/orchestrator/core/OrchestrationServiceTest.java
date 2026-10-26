package com.zaalima.orchestrator.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import com.zaalima.orchestrator.agent.Agent;
import com.zaalima.orchestrator.config.OrchestrationProperties;
import com.zaalima.orchestrator.config.TaskProperties;
import com.zaalima.orchestrator.integration.AgentClient;
import com.zaalima.orchestrator.integration.AgentDirectory;
import com.zaalima.orchestrator.integration.AgentExecutionResult;
import com.zaalima.orchestrator.integration.TaskExecutionEngine;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class OrchestrationServiceTest {

    private final ScheduledThreadPoolExecutor timer = new ScheduledThreadPoolExecutor(1);
    private final TaskService tasks = new TaskService(new TaskProperties(100), Clock.systemUTC());
    private OrchestrationService service;

    @AfterEach
    void cleanUp() {
        if (service != null) {
            service.close();
        }
        timer.shutdownNow();
    }

    private Agent agent(String id) {
        return new Agent(id, id, "test", Set.of("analytics"), Map.of(), Instant.EPOCH, Instant.EPOCH);
    }

    private OrchestrationService configure(AgentDirectory directory, AgentClient client,
                                            TaskExecutionEngine engine, Duration timeout, int maxAgents) {
        timer.setRemoveOnCancelPolicy(true);
        service = new OrchestrationService(tasks,
                new OrchestrationProperties(true, timeout, maxAgents, Duration.ofHours(1)),
                timer, directory, client, engine);
        return service;
    }

    private OrchestrationService configure(AgentDirectory directory, AgentClient client, TaskExecutionEngine engine) {
        return configure(directory, client, engine, Duration.ofSeconds(5), 16);
    }

    private AgentExecutionResult result(Agent agent, Task task) {
        return new AgentExecutionResult(task.taskId(), agent.id(), Map.of("answer", agent.id()));
    }

    @Test
    void aggregatesEveryMatchingAgentWithoutKeyCollisions() {
        configure(capability -> List.of(agent("b"), agent("a")), this::result, new InlineEngine());
        Task task = service.submit("analytics", Map.of("input", "demo"));

        assertThat(task.status()).isEqualTo(TaskStatus.SUCCEEDED);
        assertThat(service.result(task.taskId()).results()).containsExactly(
                Map.entry("a", Map.of("answer", "a")), Map.entry("b", Map.of("answer", "b")));
        assertThat(timer.getQueue()).isEmpty();
        assertThatThrownBy(() -> task.results().get("a").put("answer", "changed"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void noAgentsFailsWithAnExplicitReason() {
        configure(capability -> List.of(), this::result, new InlineEngine());
        Task task = service.submit("analytics", Map.of());
        assertThat(task.status()).isEqualTo(TaskStatus.FAILED);
        assertThat(task.failure().code()).isEqualTo("NO_AGENTS");
    }

    @Test
    void agentFailureDoesNotExposeItsExceptionMessage() {
        configure(capability -> List.of(agent("a")), (agent, task) -> {
            throw new IOException("secret-token-do-not-expose");
        }, new InlineEngine());
        Task task = service.submit("analytics", Map.of());
        assertThat(task.failure().code()).isEqualTo("AGENT_FAILURE");
        assertThat(task.failure().message()).doesNotContain("secret-token");
        assertThat(task.results()).isEmpty();
    }

    @Test
    void discoveryFailureIsReportedSafely() {
        configure(capability -> { throw new IllegalStateException("private-error"); }, this::result, new InlineEngine());
        Task task = service.submit("analytics", Map.of());
        assertThat(task.failure().code()).isEqualTo("DISCOVERY_FAILURE");
        assertThat(task.failure().message()).doesNotContain("private-error");
    }

    @Test
    void duplicateAgentIdsAreRejectedBeforeDispatch() {
        configure(capability -> List.of(agent("a"), agent("a")), this::result, new InlineEngine());
        assertThat(service.submit("analytics", Map.of()).failure().code()).isEqualTo("INVALID_AGENT_DIRECTORY");
    }

    @Test
    void agentMustSupportTheRequestedCapability() {
        configure(capability -> List.of(agent("a")), this::result, new InlineEngine());
        assertThat(service.submit("profiles", Map.of()).failure().code()).isEqualTo("INVALID_AGENT_DIRECTORY");
    }

    @Test
    void fanOutLimitDoesNotSilentlyChooseOrLoadBalanceAgents() {
        configure(capability -> List.of(agent("a"), agent("b")), this::result,
                new InlineEngine(), Duration.ofSeconds(5), 1);
        assertThat(service.submit("analytics", Map.of()).failure().code()).isEqualTo("AGENT_LIMIT_EXCEEDED");
    }

    @Test
    void wrongTaskCorrelationCannotBecomeASuccessfulResult() {
        configure(capability -> List.of(agent("a")), (agent, task) ->
                new AgentExecutionResult(UUID.randomUUID(), agent.id(), Map.of()), new InlineEngine());
        assertThat(service.submit("analytics", Map.of()).failure().code()).isEqualTo("INVALID_AGENT_RESULT");
    }

    @Test
    void wrongAgentCorrelationCannotBecomeASuccessfulResult() {
        configure(capability -> List.of(agent("a")), (agent, task) ->
                new AgentExecutionResult(task.taskId(), "wrong-agent", Map.of()), new InlineEngine());
        assertThat(service.submit("analytics", Map.of()).failure().code()).isEqualTo("INVALID_AGENT_RESULT");
    }

    @Test
    void oversizedAgentOutputIsRejected() {
        configure(capability -> List.of(agent("a")), (agent, task) ->
                new AgentExecutionResult(task.taskId(), agent.id(), Map.of("output", "x".repeat(8193))), new InlineEngine());
        assertThat(service.submit("analytics", Map.of()).failure().code()).isEqualTo("INVALID_AGENT_RESULT");
    }

    @Test
    void totalAggregatedOutputHasABoundedBudget() {
        List<Agent> agents = java.util.stream.IntStream.range(0, 9).mapToObj(i -> agent("agent-" + i)).toList();
        configure(capability -> agents, (agent, task) ->
                new AgentExecutionResult(task.taskId(), agent.id(), Map.of("output", "x".repeat(8192))), new InlineEngine());
        Task task = service.submit("analytics", Map.of());
        assertThat(task.failure().code()).isEqualTo("RESULT_LIMIT_EXCEEDED");
        assertThat(task.results()).isEmpty();
    }

    @Test
    void engineRejectionDoesNotLeaveTaskQueuedForever() {
        TaskExecutionEngine rejecting = new TaskExecutionEngine() {
            @Override
            public <T> CompletableFuture<T> submit(Callable<T> work) {
                throw new RejectedExecutionException();
            }
        };
        configure(capability -> List.of(agent("a")), this::result, rejecting);
        Task task = service.submit("analytics", Map.of());
        assertThat(task.status()).isEqualTo(TaskStatus.FAILED);
        assertThat(task.failure().code()).isEqualTo("EXECUTION_REJECTED");
        assertThat(timer.getQueue()).isEmpty();
    }

    @Test
    void cancellationBeforeDispatchPreventsAgentCalls() {
        ManualEngine engine = new ManualEngine();
        configure(capability -> List.of(agent("a")), (agent, task) -> {
            throw new AssertionError("Cancelled work must not reach the agent.");
        }, engine);
        Task task = service.submit("analytics", Map.of());
        assertThat(task.status()).isEqualTo(TaskStatus.QUEUED);
        assertThat(service.cancel(task.taskId()).status()).isEqualTo(TaskStatus.CANCELLED);
        engine.run(0);
        assertThat(tasks.findById(task.taskId()).status()).isEqualTo(TaskStatus.CANCELLED);
        assertThat(timer.getQueue()).isEmpty();
    }

    @Test
    void lateCompletionCannotOverwriteRunningTaskCancellation() {
        ManualEngine engine = new ManualEngine();
        configure(capability -> List.of(agent("a")), this::result, engine);
        Task task = service.submit("analytics", Map.of());
        engine.run(0);
        assertThat(tasks.findById(task.taskId()).status()).isEqualTo(TaskStatus.RUNNING);
        service.cancel(task.taskId());
        engine.complete(1, new AgentExecutionResult(task.taskId(), "a", Map.of("late", "result")));
        assertThat(tasks.findById(task.taskId()).status()).isEqualTo(TaskStatus.CANCELLED);
        assertThat(service.result(task.taskId()).results()).isEmpty();
    }

    @Test
    void timeoutCoversDispatchThatNeverStartsAndIgnoresLateCompletion() {
        ManualEngine engine = new ManualEngine();
        configure(capability -> List.of(agent("a")), this::result, engine, Duration.ofMillis(60), 16);
        Task task = service.submit("analytics", Map.of());
        await().atMost(Duration.ofSeconds(3)).untilAsserted(() ->
                assertThat(tasks.findById(task.taskId()).status()).isEqualTo(TaskStatus.FAILED));
        engine.run(0);
        assertThat(service.result(task.taskId()).failure().code()).isEqualTo("TIMED_OUT");
    }

    @Test
    void failFastCancelsSiblingWorkAndNeverPublishesPartialResults() {
        ManualEngine engine = new ManualEngine();
        configure(capability -> List.of(agent("a"), agent("b")), this::result, engine);
        Task task = service.submit("analytics", Map.of());
        engine.run(0);
        engine.futures.get(1).completeExceptionally(new IOException("test failure"));
        engine.complete(2, new AgentExecutionResult(task.taskId(), "b", Map.of("late", "value")));
        assertThat(service.result(task.taskId()).status()).isEqualTo(TaskStatus.FAILED);
        assertThat(service.result(task.taskId()).results()).isEmpty();
    }

    @Test
    void resultIsNotReadyForActiveTasksAndCompletedTasksCannotBeCancelled() {
        ManualEngine engine = new ManualEngine();
        configure(capability -> List.of(agent("a")), this::result, engine);
        Task task = service.submit("analytics", Map.of());
        assertThatThrownBy(() -> service.result(task.taskId())).isInstanceOf(TaskConflictException.class);
        engine.run(0);
        engine.run(1);
        assertThat(service.result(task.taskId()).status()).isEqualTo(TaskStatus.SUCCEEDED);
        assertThatThrownBy(() -> service.cancel(task.taskId())).isInstanceOf(TaskConflictException.class);
    }

    @Test
    void shutdownCancelsActiveTasksAndRejectsNewSubmissions() {
        configure(capability -> List.of(agent("a")), this::result, new ManualEngine());
        Task task = service.submit("analytics", Map.of());
        service.close();
        assertThat(tasks.findById(task.taskId()).status()).isEqualTo(TaskStatus.CANCELLED);
        assertThatThrownBy(() -> service.submit("analytics", Map.of())).isInstanceOf(TaskCapacityException.class);
    }

    @Test
    void disabledModeQueuesWithoutPretendingAdaptersExist() {
        service = new OrchestrationService(tasks, OrchestrationProperties.defaults(), timer, null, null, null);
        assertThat(service.submit("analytics", Map.of()).status()).isEqualTo(TaskStatus.QUEUED);
    }

    @Test
    void enabledExecutionWithoutProductionPortsFailsClearly() {
        assertThatThrownBy(() -> new OrchestrationService(tasks,
                new OrchestrationProperties(true, Duration.ofSeconds(1), 1, Duration.ofHours(1)),
                timer, null, null, null)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AgentDirectory", "AgentClient", "TaskExecutionEngine");
    }

    private static class InlineEngine implements TaskExecutionEngine {
        @Override
        public <T> CompletableFuture<T> submit(Callable<T> work) {
            try {
                return CompletableFuture.completedFuture(work.call());
            } catch (Exception exception) {
                return CompletableFuture.failedFuture(exception);
            }
        }
    }

    // Deliberately ignores cancel to verify core terminal states survive a badly behaved adapter.
    private static class ManualEngine implements TaskExecutionEngine {
        private final List<Runnable> work = new ArrayList<>();
        private final List<CompletableFuture<?>> futures = new ArrayList<>();

        @Override
        public <T> CompletableFuture<T> submit(Callable<T> callable) {
            CompletableFuture<T> completion = new CompletableFuture<>() {
                @Override
                public boolean cancel(boolean interrupt) {
                    return false;
                }
            };
            futures.add(completion);
            work.add(() -> {
                try {
                    completion.complete(callable.call());
                } catch (Exception exception) {
                    completion.completeExceptionally(exception);
                }
            });
            return completion;
        }

        void run(int index) {
            work.get(index).run();
        }

        @SuppressWarnings("unchecked")
        void complete(int index, AgentExecutionResult result) {
            ((CompletableFuture<AgentExecutionResult>) futures.get(index)).complete(result);
        }
    }
}
