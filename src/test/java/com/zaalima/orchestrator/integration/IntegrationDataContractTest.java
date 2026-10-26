package com.zaalima.orchestrator.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.zaalima.orchestrator.core.Task;
import com.zaalima.orchestrator.core.TaskStatus;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IntegrationDataContractTest {

    @Test
    void eventContainsCancellationTimeAndCorrelationWithoutPayload() throws Exception {
        Instant submitted = Instant.parse("2026-10-18T09:00:00Z");
        Instant cancelled = submitted.plusSeconds(5);
        Task task = new Task(UUID.randomUUID(), "analytics", Map.of("private-input", "not-for-pubsub"),
                TaskStatus.CANCELLED, submitted, cancelled);
        TaskEvent event = TaskEvent.from(task);

        assertThat(event.taskId()).isEqualTo(task.taskId());
        assertThat(event.status()).isEqualTo(TaskStatus.CANCELLED);
        assertThat(event.occurredAt()).isEqualTo(cancelled);
        assertThat(event.schemaVersion()).isEqualTo(2);
        assertThat(TaskEvent.from(task).eventId()).isNotEqualTo(event.eventId());
        ObjectMapper mapper = JsonMapper.builder().findAndAddModules().build();
        String json = mapper.writeValueAsString(event);
        assertThat(json).doesNotContain("private-input", "not-for-pubsub", "payload");
        assertThat(mapper.readValue(json, TaskEvent.class)).isEqualTo(event);
    }

    @Test
    void unsupportedEventSchemaIsRejected() {
        assertThatThrownBy(() -> new TaskEvent(UUID.randomUUID(), 99, UUID.randomUUID(),
                TaskStatus.QUEUED, Instant.now())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void incompleteEventCannotCrossTheBoundary() {
        assertThatThrownBy(() -> new TaskEvent(null, 1, UUID.randomUUID(), TaskStatus.QUEUED, Instant.now()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new TaskEvent(UUID.randomUUID(), 1, null, TaskStatus.QUEUED, Instant.now()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new TaskEvent(UUID.randomUUID(), 1, UUID.randomUUID(), null, Instant.now()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new TaskEvent(UUID.randomUUID(), 1, UUID.randomUUID(), TaskStatus.QUEUED, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void executionResultCopiesAgentOutput() {
        Map<String, String> output = new HashMap<>(Map.of("value", "original"));
        AgentExecutionResult result = new AgentExecutionResult(UUID.randomUUID(), "agent-1", output);
        output.put("value", "changed");

        assertThat(result.output()).containsEntry("value", "original");
        assertThatThrownBy(() -> result.output().put("extra", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void legacyEventSchemaKeepsItsOriginalStatusSet() {
        assertThat(new TaskEvent(UUID.randomUUID(), 1, UUID.randomUUID(), TaskStatus.QUEUED, Instant.now())
                .schemaVersion()).isEqualTo(1);
        assertThatThrownBy(() -> new TaskEvent(UUID.randomUUID(), 1, UUID.randomUUID(),
                TaskStatus.RUNNING, Instant.now())).isInstanceOf(IllegalArgumentException.class);
    }
}
