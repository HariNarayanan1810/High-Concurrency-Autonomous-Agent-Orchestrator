package com.zaalima.orchestrator.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.zaalima.orchestrator.config.OrchestrationProperties;
import com.zaalima.orchestrator.config.TaskProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class TaskRetentionTest {

    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-10-26T12:00:00Z"));

    private TaskService service(int capacity) {
        Clock clock = mock(Clock.class);
        when(clock.instant()).thenAnswer(invocation -> now.get());
        return new TaskService(new TaskProperties(capacity), clock,
                new OrchestrationProperties(false, Duration.ofSeconds(1), 1, Duration.ofMinutes(10)));
    }

    @Test
    void expiredTerminalTaskReleasesCapacityOnAdmission() {
        TaskService service = service(1);
        Task original = service.submit("analytics", Map.of());
        service.cancel(original.taskId());
        now.updateAndGet(time -> time.plus(Duration.ofMinutes(10)));
        assertThat(service.submit("analytics", Map.of()).taskId()).isNotEqualTo(original.taskId());
        assertThatThrownBy(() -> service.findById(original.taskId())).isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void activeTasksAreNeverEvictedToAcceptNewWork() {
        TaskService service = service(2);
        Task queued = service.submit("analytics", Map.of());
        Task running = service.submit("analytics", Map.of());
        service.start(running.taskId());
        now.updateAndGet(time -> time.plus(Duration.ofDays(1)));
        assertThat(service.purgeExpired()).isZero();
        assertThatThrownBy(() -> service.submit("analytics", Map.of())).isInstanceOf(TaskCapacityException.class);
        assertThat(service.findById(queued.taskId()).status()).isEqualTo(TaskStatus.QUEUED);
        assertThat(service.findById(running.taskId()).status()).isEqualTo(TaskStatus.RUNNING);
    }

    @Test
    void repeatedCleanupCannotReleaseTheSameSlotTwice() {
        TaskService service = service(1);
        Task original = service.submit("analytics", Map.of());
        service.start(original.taskId());
        service.succeed(original.taskId(), Map.of("agent", Map.of("answer", "ok")));
        now.updateAndGet(time -> time.plus(Duration.ofMinutes(11)));
        assertThat(service.purgeExpired()).isEqualTo(1);
        assertThat(service.purgeExpired()).isZero();
        service.submit("analytics", Map.of());
        assertThatThrownBy(() -> service.submit("analytics", Map.of())).isInstanceOf(TaskCapacityException.class);
    }
}
