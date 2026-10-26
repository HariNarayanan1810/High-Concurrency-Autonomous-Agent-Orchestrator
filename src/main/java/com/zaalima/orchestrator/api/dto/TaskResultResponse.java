package com.zaalima.orchestrator.api.dto;

import com.zaalima.orchestrator.core.Task;
import com.zaalima.orchestrator.core.TaskFailure;
import com.zaalima.orchestrator.core.TaskStatus;
import java.util.Map;
import java.util.UUID;

public record TaskResultResponse(UUID taskId, TaskStatus status,
                                 Map<String, Map<String, String>> results, TaskFailure failure) {

    public static TaskResultResponse from(Task task) {
        return new TaskResultResponse(task.taskId(), task.status(), task.results(), task.failure());
    }
}
