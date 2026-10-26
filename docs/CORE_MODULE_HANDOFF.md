# Core Module Handoff

Updated: 2026-10-26. HARI's core implementation is ready for review and adapter integration. This is not full-project completion.

## What Is Implemented

- POST /api/v1/tasks: validated admission and optional dispatch.
- GET /api/v1/tasks/{id}: immutable lifecycle snapshot.
- DELETE /api/v1/tasks/{id}: queued/running cancellation; repeat cancellation is idempotent; succeeded/failed tasks return 409.
- GET /api/v1/tasks/{id}/result: terminal result/failure; active task 409; unknown/expired ID 404.
- Agent fan-out, result correlation and bounded aggregation, fail-fast behavior, task timeout, late-result protection, shutdown cancellation, and bounded retained storage.
- Core unit/HTTP tests using labeled test doubles; all existing teammate tests retained.

## Required Adapters

| Owner | Required Beans | Acceptance Before Enabling |
| --- | --- | --- |
| Robin | TaskExecutionEngine | Nonblocking submit; exception propagation; rejection; cancellation requests reach underlying work; no worker waits for child joins; shutdown and preview requirements tested |
| Ahamed | AgentDirectory, AgentClient | Healthy matching candidates; stable snapshots; authenticated/time-bounded transport; result taskId/agentId match the request; interruption propagated |
| Navitha | CoordinationLock, TaskEventPublisher for later distributed integration | Ownership-token/TTL/release safety, real Redis tests, schema/reconnect semantics; not substituted by in-memory core storage |

Set TASK_EXECUTION_ENABLED=true only after all three execution adapter beans are registered. Application startup fails if any are missing. Default false leaves foundation tasks queued, with no fabricated execution. The existing in-memory AgentRegistry is not an AgentDirectory/AgentClient implementation.

## Aggregation Policy

A task fans out the same capability/payload to every matching available agent up to MAX_AGENTS_PER_TASK. More candidates fail with AGENT_LIMIT_EXCEEDED; core does not implement the unassigned balancing algorithm. All agents must succeed. Output is nested by agent ID. Any error/timeout/cancellation yields no partial successful aggregate.

Result checks reject mismatched task/agent IDs and oversized output. Timeout includes engine queueing and discovery, not just client time. There are no automatic retries because remote side effects/idempotency have not been specified.

## Cancellation Contract

Core always protects terminal task state from late callbacks. It calls future.cancel(true) for outstanding work. CompletableFuture alone does not guarantee interrupting its underlying operation: Robin must implement that behavior, Ahamed must honor interruption and I/O timeouts. Remote agent side effects require their own cancellation/idempotency protocol.

One local timer thread manages deadlines only; it does not replace Robin's production engine. Timer handles are cancelled and removed when work terminates.

## Verification

In both the isolated copy and active C:\IMP project: mvn -B -ntp clean verify -Ddebug=false -Dlogging.level.org.springframework=INFO passed 71 tests with no failures/errors/skips. This includes 8 real HTTP execution tests using TestExecutionEngine and deterministic test agents, 20 orchestration cases, retention/configuration tests, and Ahamed's 12 existing registry tests. All 9 Git-hook checks passed in both locations. Active packaged-JAR smoke verified default-mode health/admission/pending-result/cancellation; its temporary process was stopped.

Run just the simulated HTTP workflow with mvn -Dtest=OrchestrationApiIntegrationTest test. Test adapters exist only under src/test; they are not packaged as production beans.

## Configuration

| Variable | Default | Meaning |
| --- | --- | --- |
| TASK_EXECUTION_ENABLED | false | Requires real adapters when true |
| TASK_TIMEOUT | 30s | Whole task deadline; 1ms-24h |
| MAX_AGENTS_PER_TASK | 16 | Fan-out cap; 1-64 |
| TASK_RETENTION | 1h | Minimum age of terminal records eligible for capacity reclamation |
| MAX_STORED_TASKS | 1000 | Total retained task records |
| SERVER_ADDRESS / SERVER_PORT | 127.0.0.1 / 8080 | Development binding |

Retention cleanup is admission-driven, not an exact periodic expiry guarantee. State is not durable or distributed. Queue-only mode can fill capacity with active queued tasks; no active task is evicted.

## Remaining Integration Work

Obtain/review actual adapters, agree event schema v2/new states, enable execution, rerun integration against real agents/Redis, and prepare the final demo. Keep Week 3/4 unassigned requirements with the team for ownership decisions. CI has a PowerShell-runtime adjustment but needs a remote rerun after push. GitHub branch protection is a repository setting, not a missing Java feature.
