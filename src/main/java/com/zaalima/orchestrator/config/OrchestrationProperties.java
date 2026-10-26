package com.zaalima.orchestrator.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("orchestrator.execution")
public record OrchestrationProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("PT30S") Duration timeout,
        @DefaultValue("16") int maxAgents,
        @DefaultValue("PT1H") Duration retention) {

    public OrchestrationProperties {
        if (timeout == null || timeout.compareTo(Duration.ofMillis(1)) < 0
                || timeout.compareTo(Duration.ofDays(1)) > 0) {
            throw new IllegalArgumentException("Execution timeout must be between 1ms and 24h.");
        }
        if (maxAgents < 1 || maxAgents > 64) {
            throw new IllegalArgumentException("max-agents must be between 1 and 64.");
        }
        if (retention == null || retention.compareTo(Duration.ofMillis(1)) < 0) {
            throw new IllegalArgumentException("Task retention must be at least 1ms.");
        }
    }

    public static OrchestrationProperties defaults() {
        return new OrchestrationProperties(false, Duration.ofSeconds(30), 16, Duration.ofHours(1));
    }
}
