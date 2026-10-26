# Architecture

Updated: 2026-10-26.

## Implemented Core

- Thin task/health REST controllers and validation/ProblemDetail exception handling.
- TaskService owns bounded process-local storage, immutable snapshots, lifecycle transitions, and retention.
- OrchestrationService coordinates discovery, engine submission, per-agent calls, result validation/aggregation, deadlines, and cancellation.
- OrchestrationConfiguration supplies execution settings and one daemon scheduling thread for timeout bookkeeping. This timer does not execute agent work and is not Robin's engine.
- AgentDirectory/AgentClient/TaskExecutionEngine remain production adapter boundaries.
- Ahamed's agent model/registry implementation and its tests are unchanged.
- CoordinationLock/TaskEventPublisher are defined but are not used to claim distributed admission or reliable event delivery.

## Execution Flow

1. Controller validates POST /api/v1/tasks.
2. Admission reserves bounded storage and creates QUEUED state.
3. If execution is disabled, return the queued snapshot without starting work.
4. When enabled, schedule a task-wide deadline and submit discovery through the injected execution engine.
5. Atomically start RUNNING, obtain all matching available agents, validate the candidate set, and enforce the fan-out cap.
6. Submit one AgentClient call per agent. The coordinator never joins child work on an engine worker.
7. Validate task/agent correlation and output limits. Aggregate separately by agent ID to avoid overwriting identical output keys.
8. All agents succeeding commits one immutable SUCCEEDED snapshot. Any failure commits FAILED, discards partial aggregate, and requests cancellation of outstanding futures.
9. Timeout commits FAILED/TIMED_OUT. Cancellation commits CANCELLED and requests future cancellation. Atomic terminal guards prevent late callbacks from overwriting a winner.

This is fan-out orchestration, not least-utilized-node balancing. It does not implement a workflow DAG or Structured Concurrency engine.

## Storage and Races

ConcurrentHashMap.compute transitions are short and contain no transport calls. Terminal snapshots are immutable. Cancelling a succeeded/failed task returns HTTP 409; repeated cancellation is idempotent.

Expired terminal records are reclaimed when submission hits capacity. Queued/running work is never evicted for admission. Data is lost on restart and is not shared across nodes. Locks, durable recovery, and distributed exactly-once execution are not implemented.

## Limits and Security

Default max agents/task: 16, configurable 1-64. Per-agent output: at most 64 entries, nonblank keys up to 128 characters, values up to 8192 characters. Combined result budget: 65,536 key/value Java string characters per task. Capability/payload limits remain enforced at the API.

The API binds to localhost by default and has no public deployment authentication. Task errors return fixed public messages/codes, not agent exception messages. Real AgentClient implementations must enforce transport authentication/timeouts. Cancellation correctness of core state does not imply that a remote side effect can be undone.

## Verification

Full-context HTTP tests run enabled execution with explicitly labeled test adapters; normal application mode installs none. Tests cover aggregation, correlation/failure, cancellation/interruption, deadline, retention, and existing validation/concurrency behavior. No distributed benchmark or 10,000+ connection claim is made.
