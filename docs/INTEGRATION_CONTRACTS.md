# Integration Contracts

Updated: 2026-10-26. Core orchestration now consumes discovery/client/execution ports. Production adapters remain with their owners; no production simulation is wired. Ahamed's process-local registry is not a discovery/communication adapter. See CORE_MODULE_HANDOFF.md.

## Implemented REST APIs

| Method | Path | Success | Behavior |
| --- | --- | --- | --- |
| GET | /api/v1/health | 200 | Service status, name, version, UTC timestamp |
| POST | /api/v1/tasks | 202 | Accept task; Location identifies status URL; enabled execution may already be running/terminal when returned |
| GET | /api/v1/tasks/{taskId} | 200 | Return a task snapshot |
| DELETE | /api/v1/tasks/{taskId} | 200 | Cancel queued/running work; repeated cancellation is idempotent |
| GET | /api/v1/tasks/{taskId}/result | 200 | Terminal output grouped by agent ID, sanitized failure, or cancellation |

Submission example:

```json
{"capability":"analytics","payload":{"input":"demo"}}
```

- capability: required, 1-64 characters; starts with an ASCII letter/digit, followed by letters/digits, dot, underscore, or hyphen. Case-sensitive.
- payload: required string-to-string object with up to 16 entries; may be empty. Keys must be nonblank and at most 64 characters. Values must be non-null and at most 1024 characters. These are Java string-length limits.
- capability is a routing label, not a class name or executable command.

TaskResponse fields: UUID taskId, String capability, Map<String,String> payload, TaskStatus status, Instant submittedAt, Instant updatedAt. Timestamps use ISO-8601 UTC.

TaskResultResponse fields: taskId, status, results (agent ID -> output map), failure (null or code/message).

Errors use application/problem+json: 400 for invalid input/UUID/JSON, 404 for unknown/expired tasks, 409 for pending results or cancelling succeeded/failed tasks, 503 for full storage/service closing. Validation errors contain field/message pairs, not rejected values.

Enabled execution: QUEUED -> RUNNING -> SUCCEEDED/FAILED; queued/running tasks can become CANCELLED. Deadline covers queueing and discovery. Default disabled mode preserves QUEUED/cancellation without adapters. Enabling without all three required adapter beans fails startup.

Storage is process-local and lost at restart. Full admission reclaims terminal snapshots older than retention; active tasks are never evicted. Repeated POST creates distinct tasks; no idempotency keys or durable recovery are implemented.

## Java Ports

Package: com.zaalima.orchestrator.integration.

| Interface | Production Owner | Input / Output | Required Behavior |
| --- | --- | --- | --- |
| AgentDirectory.findAvailable(String capability) | Ahamed | Capability -> List<Agent> | Registered healthy agents supporting that exact capability, or an empty list; never null. Return stable snapshots; exclude stale heartbeats. Discovery does not implement least-utilized-node balancing. |
| AgentClient.execute(Agent agent, Task task) | Ahamed | Agent + immutable task -> AgentExecutionResult | Blocking I/O suitable for Robin's virtual threads. Enforce transport timeouts/authentication. Return matching taskId/agentId and immutable output. Propagate I/O failure as IOException and interruption as InterruptedException. |
| TaskExecutionEngine.submit(Callable<T> work) | Robin | Work -> CompletableFuture<T> | Return promptly; submit asynchronously, completing with result or exception. Reject unavailable capacity with RejectedExecutionException. Bridge cancellation to underlying work; test interruption/shutdown before integration. No production executor is implemented here. |

AgentExecutionResult contains UUID taskId, String agentId, and Map<String,String> output, copied at construction. Core validates task/agent correlation and aggregates only after every matching agent succeeds. Failures discard partial output. Results are ordered by agent ID. No retries are introduced.

Core snapshots discovered agent collections and requires unique nonblank IDs up to 128 characters and exact capability support. It broadcasts to all candidates within MAX_AGENTS_PER_TASK; exceeding the cap fails instead of silently selecting agents. This is not load balancing. Each result permits at most 64 entries, nonblank keys up to 128 characters, values up to 8192; the combined task output budget is 65,536 key/value Java string characters.

Failure/cancellation/deadline requests cancel(true) on tracked futures. CompletableFuture cancellation alone does not guarantee interruption: Robin must bridge it to real work, and Ahamed must honor interruption and transport timeouts. Late completion cannot overwrite a terminal task, but uncooperative remote side effects cannot be forcibly stopped. The scheduler is only a timer, not Robin's executor.

Public failure codes: TIMED_OUT, EXECUTION_REJECTED, NO_AGENTS, AGENT_LIMIT_EXCEEDED, INVALID_AGENT_DIRECTORY, AGENT_FAILURE, INVALID_AGENT_RESULT, RESULT_LIMIT_EXCEEDED, DISCOVERY_FAILURE. Fixed messages exclude raw exception details/credentials.

Ahamed's Agent, AgentRegistry, and October 17 InMemoryAgentRegistry remain unchanged. The in-memory implementation deduplicates by name and provides registration/ID lookup/heartbeat/removal, without capability discovery or transport endpoints. Its 12 tests pass. Agree heartbeat expiry, endpoint metadata, REST/gRPC payloads, and security before wiring directory/client adapters.

## Redis Java Ports

- CoordinationLock.tryAcquire(String resource, Duration ttl): returns Optional<LockLease>; empty means contended, not infrastructure failure. Resource must be nonblank and TTL positive. Redis/network failures throw IOException; interruption throws InterruptedException. Implement with a fresh unpredictable ownership token per acquisition and atomic create-with-TTL. This call does not wait for an existing holder.
- LockLease provides resource(), ownershipToken(), expiresAt(), and close(). Close must be idempotent and atomically delete only if the stored token still matches. Closing an expired lease must never release a replacement holder. No implicit renewal is specified. Expiry ends exclusive ownership: future orchestration must bound work by the lease, handle expiry, and cannot assume a TTL alone fences stale workers.
- Proposed task-lock resource: task:{UUID}; Navitha owns Redis namespace mapping and clock/expiry implementation. TTL values and renewal/fencing policy still need team agreement before orchestration uses locks.
- TaskEventPublisher.publish(TaskEvent): sends one versioned event or throws IOException. Success means publish accepted, not delivery/processing acknowledgment. Pub/Sub is not durable and there is no exactly-once or ordering guarantee across publishers.
- TaskEvent JSON fields: eventId, schemaVersion, taskId, status, occurredAt. TaskEvent.from(task) now emits version 2 for the expanded lifecycle and uses task.updatedAt; events exclude payload, agent secrets, and results. Reuse eventId on retries if deduplication is implemented later.
- Version 1 accepts only QUEUED/CANCELLED. Version 2 accepts all five current statuses; unsupported versions are rejected. Coordinate consumer rollout before publishing v2. Timestamps use ISO-8601 UTC; JSON compatibility/validation tests are implemented.
- No production lock, Pub/Sub adapter, or consumer is implemented here, and TaskService does not call the ports yet. A durable outbox/recovery design is a separate future decision.

### Handoff Acceptance

- Robin: implement submit against the documented threading/failure/cancellation contract and supply executor-specific tests; no production executor is supplied by HARI.
- Ahamed: supply discovery and client adapters plus secure REST and gRPC registration schemas, authentication/authorization, duplicate-name policy, heartbeat expiry, transport address, and failure mapping. The PDF requires both interface designs; it does not specify an authentication mechanism. These decisions remain with his module and team review.
- Navitha: test contention, expired lease/replacement ownership, idempotent release, positive TTL validation, Redis outage/interruption, and reconnect/message loss. Use actual Redis for production evidence.
- HARI: core dispatch/result correlation is implemented and tested with labeled doubles. Consume approved production adapters and resolve contract mismatches with owners; teammate acceptance is not fabricated.

## Implemented Simulated-Agent Tests

OrchestrationApiIntegrationTest supplies two deterministic test agents and a test-only executor with delay/failure/cancellation scenarios. Eight real HTTP tests verify execution APIs without claiming production transport/executor integration or load-target compliance. No demo/production fallback is supplied. The PDF's analytics/profile examples do not define a business payload schema.

## Configuration and Limits

- SERVER_ADDRESS: defaults to 127.0.0.1 for this unauthenticated development API.
- SERVER_PORT: defaults to 8080.
- MAX_STORED_TASKS: defaults to 1000; minimum 1.
- TASK_EXECUTION_ENABLED: false; enabling requires discovery/client/execution beans.
- TASK_TIMEOUT: 30s, between 1ms and 24h; MAX_AGENTS_PER_TASK: 16, range 1-64.
- TASK_RETENTION: 1h, minimum 1ms; cleanup is admission-driven when full, not periodic deletion.
- Persistence and public deployment security remain pending.
