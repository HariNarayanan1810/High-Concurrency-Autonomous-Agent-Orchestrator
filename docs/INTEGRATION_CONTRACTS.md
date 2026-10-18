# Integration Contracts

Updated: 2026-10-18. Core ports are defined for teammate review. No production execution or coordination adapter is wired yet. Ahamed's process-local registry implementation exists but is not a discovery/communication adapter.

## Implemented REST APIs

| Method | Path | Success | Behavior |
| --- | --- | --- | --- |
| GET | /api/v1/health | 200 | Service status, name, version, UTC timestamp |
| POST | /api/v1/tasks | 202 | Store a QUEUED task; Location header identifies its status URL |
| GET | /api/v1/tasks/{taskId} | 200 | Return a task snapshot |
| DELETE | /api/v1/tasks/{taskId} | 200 | Cancel a queued task; repeated cancellation returns the same snapshot |

Submission example:

```json
{"capability":"analytics","payload":{"input":"demo"}}
```

- capability: required, 1-64 characters; starts with an ASCII letter/digit, followed by letters/digits, dot, underscore, or hyphen. Case-sensitive.
- payload: required string-to-string object with up to 16 entries; may be empty. Keys must be nonblank and at most 64 characters. Values must be non-null and at most 1024 characters. These are Java string-length limits.
- capability is a routing label, not a class name or executable command.

TaskResponse fields: UUID taskId, String capability, Map<String,String> payload, TaskStatus status, Instant submittedAt, Instant updatedAt. Timestamps use ISO-8601 UTC.

Errors use application/problem+json: 400 for invalid input/UUID/JSON, 404 for an unknown task, 503 when storage is full. Validation errors contain an errors array of field/message pairs. Rejected values are not included in the response.

Only QUEUED and CANCELLED are implemented. Tasks are not dispatched, executed, timed out, or aggregated. Cancellation preserves the record for lookup. Cancelled records still count toward capacity. State is process-local and lost at restart. Repeated POST requests create distinct tasks; idempotency keys are not implemented.

## Java Ports

Package: com.zaalima.orchestrator.integration.

| Interface | Production Owner | Input / Output | Required Behavior |
| --- | --- | --- | --- |
| AgentDirectory.findAvailable(String capability) | Ahamed | Capability -> List<Agent> | Registered healthy agents supporting that exact capability, or an empty list; never null. Return stable snapshots; exclude stale heartbeats. Discovery does not implement least-utilized-node balancing. |
| AgentClient.execute(Agent agent, Task task) | Ahamed | Agent + immutable task -> AgentExecutionResult | Blocking I/O suitable for Robin's virtual threads. Enforce transport timeouts/authentication. Return matching taskId/agentId and immutable output. Propagate I/O failure as IOException and interruption as InterruptedException. |
| TaskExecutionEngine.submit(Callable<T> work) | Robin | Work -> CompletableFuture<T> | Submit asynchronously, completing with result or exception. Reject unavailable capacity with RejectedExecutionException. Agree/test cancellation, interruption, and shutdown before integration. No executor is implemented here. |

AgentExecutionResult contains UUID taskId, String agentId, and Map<String,String> output, copied at construction. It represents success; failures use the client exception contract. Result validation against the dispatched task and aggregation remain HARI's future work.

Ahamed's Agent, AgentRegistry, and October 17 InMemoryAgentRegistry remain unchanged. The in-memory implementation deduplicates by name and provides registration/ID lookup/heartbeat/removal, without capability discovery or transport endpoints. Its 12 tests pass. Agree heartbeat expiry, endpoint metadata, REST/gRPC payloads, and security before wiring directory/client adapters.

## Redis Java Ports

- CoordinationLock.tryAcquire(String resource, Duration ttl): returns Optional<LockLease>; empty means contended, not infrastructure failure. Resource must be nonblank and TTL positive. Redis/network failures throw IOException; interruption throws InterruptedException. Implement with a fresh unpredictable ownership token per acquisition and atomic create-with-TTL. This call does not wait for an existing holder.
- LockLease provides resource(), ownershipToken(), expiresAt(), and close(). Close must be idempotent and atomically delete only if the stored token still matches. Closing an expired lease must never release a replacement holder. No implicit renewal is specified. Expiry ends exclusive ownership: future orchestration must bound work by the lease, handle expiry, and cannot assume a TTL alone fences stale workers.
- Proposed task-lock resource: task:{UUID}; Navitha owns Redis namespace mapping and clock/expiry implementation. TTL values and renewal/fencing policy still need team agreement before orchestration uses locks.
- TaskEventPublisher.publish(TaskEvent): sends one versioned event or throws IOException. Success means publish accepted, not delivery/processing acknowledgment. Pub/Sub is not durable and there is no exactly-once or ordering guarantee across publishers.
- TaskEvent v1 JSON fields: UUID eventId, int schemaVersion=1, UUID taskId, TaskStatus status, Instant occurredAt. TaskEvent.from(task) uses task.updatedAt; the event does not include request payload, agent secrets, or results. Reuse the same eventId on retry so receivers can deduplicate if implemented later.
- Schema v1 accepts only current QUEUED/CANCELLED status values. Agree compatibility before introducing execution states or fields. Timestamp uses ISO-8601 UTC. Event JSON round-trip/validation tests are implemented.
- No production lock, Pub/Sub adapter, or consumer is implemented here, and TaskService does not call the ports yet. A durable outbox/recovery design is a separate future decision.

### Handoff Acceptance

- Robin: implement submit against the documented threading/failure/cancellation contract and supply executor-specific tests; no production executor is supplied by HARI.
- Ahamed: supply discovery and client adapters plus secure REST and gRPC registration schemas, authentication/authorization, duplicate-name policy, heartbeat expiry, transport address, and failure mapping. The PDF requires both interface designs; it does not specify an authentication mechanism. These decisions remain with his module and team review.
- Navitha: test contention, expired lease/replacement ownership, idempotent release, positive TTL validation, Redis outage/interruption, and reconnect/message loss. Use actual Redis for production evidence.
- HARI: consume approved adapters during orchestration work and test dispatch/result correlation. Signatures of these ports are defined; teammate acceptance is not fabricated.

## Proposed Simulated-Agent MVP

The PDF mentions analytics/profile fetching but does not define a business payload schema. Proposed next test fixture: deterministic analytics and profiles agents with configurable delay/failure. Keep these in tests or an explicitly approved demo profile, then verify dispatch/cancellation/aggregation without claiming production integration or load-target compliance.

## Configuration and Limits

- SERVER_ADDRESS: defaults to 127.0.0.1 for this unauthenticated development API.
- SERVER_PORT: defaults to 8080.
- MAX_STORED_TASKS: defaults to 1000; minimum 1.
- Persistence, retention eviction, and public deployment security are pending.
