package com.zaalima.orchestrator.integration;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;

public interface CoordinationLock {

    Optional<LockLease> tryAcquire(String resource, Duration ttl) throws IOException, InterruptedException;
}
