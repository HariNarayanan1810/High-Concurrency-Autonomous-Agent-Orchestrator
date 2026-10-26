package com.zaalima.orchestrator.support;

import com.zaalima.orchestrator.integration.TaskExecutionEngine;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

// Test-only adapter, not Robin's production execution engine or Structured Concurrency implementation.
public final class TestExecutionEngine implements TaskExecutionEngine, AutoCloseable {

    private final ExecutorService workers = Executors.newVirtualThreadPerTaskExecutor();

    @Override
    public <T> CompletableFuture<T> submit(Callable<T> work) {
        CompletableFuture<T> completion = new CompletableFuture<>();
        AtomicReference<Future<?>> runner = new AtomicReference<>();
        completion.whenComplete((result, error) -> {
            Future<?> submitted = runner.get();
            if (completion.isCancelled() && submitted != null) {
                submitted.cancel(true);
            }
        });
        runner.set(workers.submit(() -> {
            try {
                completion.complete(work.call());
            } catch (Exception exception) {
                completion.completeExceptionally(exception);
            }
        }));
        if (completion.isCancelled()) {
            runner.get().cancel(true);
        }
        return completion;
    }

    @Override
    public void close() {
        workers.shutdownNow();
        workers.close();
    }
}
