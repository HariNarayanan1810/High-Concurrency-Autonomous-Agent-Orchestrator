package com.zaalima.orchestrator.integration;

import java.io.IOException;
import java.time.Instant;

public interface LockLease extends AutoCloseable {

    String resource();

    String ownershipToken();

    Instant expiresAt();

    // Release must atomically match the ownership token, even if this lease expired.
    @Override
    void close() throws IOException;
}
