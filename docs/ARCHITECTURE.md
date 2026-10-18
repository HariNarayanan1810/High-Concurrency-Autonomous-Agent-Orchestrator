# Architecture

Updated: 2026-10-18.

## Implemented Components

- api: one health controller, task controller, centralized MVC/task exception handling.
- api.dto: validated task request and task/health response DTOs.
- core: immutable snapshots, QUEUED/CANCELLED lifecycle, submission/lookup/cancellation service.
- config: UTC clock, validated task capacity, Spring Boot virtual-thread setting. Build version comes from Maven-generated build-info instead of a hardcoded BuildProperties bean.
- agent: Ahamed's model, registry interface, and October 17 in-memory registry, retained unchanged.
- integration: discovery/client/execution ports plus lease-based coordination and versioned task-event ports; production adapters are not wired.

## Request Flow

1. POST /api/v1/tasks validates capability and string payload.
2. TaskService reserves capacity without blocking and stores an immutable QUEUED task in a ConcurrentHashMap.
3. Controller returns HTTP 202 with Location and a task snapshot.
4. GET returns a snapshot or HTTP 404.
5. DELETE atomically transitions QUEUED to CANCELLED. Repeated cancellation preserves the first cancellation snapshot.

Capacity applies to all stored records, including cancelled tasks. Records are not evicted, disappear at restart, and are not shared across instances. This is an in-memory API foundation, not a distributed task queue. Redis ports are defined but not invoked in this flow.

## Future Execution Flow

After contracts/adapters are approved, orchestration will discover agents through AgentDirectory, submit work through TaskExecutionEngine, invoke AgentClient, and aggregate results. Execution states, deadlines, running-task cancellation, and result APIs remain pending. Redis interfaces/event schemas require agreement with Navitha.

## Verification and Boundaries

Real HTTP RANDOM_PORT SpringBootTest suites exercise full component scanning and catch the duplicate-controller regression. A test-only filter verifies the HTTP handling thread is virtual; it adds no production header. Service tests check immutable payloads, cancellation, lookup, and concurrent capacity admission. Event contract tests verify version/correlation, UTC JSON round-trip, and payload exclusion. Ahamed's 12 registry tests also pass.

Versioned Git hooks enforce the local branch/signing protocol. Backend CI supplies Java 21 builds, hook checks, and PR commit-verification checks. GitHub protected-main settings remain a separate administrator action.

The API binds to localhost by default because authentication is pending. No pinning, leak, cluster, or 10,000-connection guarantee has been established.
