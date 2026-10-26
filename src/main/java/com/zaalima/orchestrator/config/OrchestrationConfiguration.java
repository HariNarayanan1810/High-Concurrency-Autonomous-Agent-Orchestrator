package com.zaalima.orchestrator.config;

import com.zaalima.orchestrator.core.OrchestrationService;
import com.zaalima.orchestrator.core.TaskService;
import com.zaalima.orchestrator.integration.AgentClient;
import com.zaalima.orchestrator.integration.AgentDirectory;
import com.zaalima.orchestrator.integration.TaskExecutionEngine;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OrchestrationProperties.class)
public class OrchestrationConfiguration {

    @Bean(destroyMethod = "shutdownNow")
    public ScheduledExecutorService orchestrationTimeouts() {
        ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1,
                Thread.ofPlatform().daemon().name("orchestration-timeout-", 0).factory());
        scheduler.setRemoveOnCancelPolicy(true);
        return scheduler;
    }

    @Bean(destroyMethod = "close")
    public OrchestrationService orchestrationService(TaskService tasks, OrchestrationProperties properties,
            @Qualifier("orchestrationTimeouts") ScheduledExecutorService timeouts,
            ObjectProvider<AgentDirectory> directory, ObjectProvider<AgentClient> client,
            ObjectProvider<TaskExecutionEngine> engine) {
        return new OrchestrationService(tasks, properties, timeouts,
                directory.getIfAvailable(), client.getIfAvailable(), engine.getIfAvailable());
    }
}
