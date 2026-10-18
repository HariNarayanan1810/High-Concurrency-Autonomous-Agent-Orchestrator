package com.zaalima.orchestrator.integration;

import java.io.IOException;

public interface TaskEventPublisher {

    void publish(TaskEvent event) throws IOException;
}
