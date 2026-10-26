# High-Concurrency-Autonomous-Agent-Orchestrator

## Local Development

Requires Java 21 and Maven 3.9+. From the project directory:

```powershell
mvn clean verify
mvn spring-boot:run
```

Health endpoint: http://127.0.0.1:8080/api/v1/health

```powershell
$task = Invoke-RestMethod -Method Post -Uri http://127.0.0.1:8080/api/v1/tasks -ContentType application/json -Body '{"capability":"analytics","payload":{"input":"demo"}}'
Invoke-RestMethod -Uri "http://127.0.0.1:8080/api/v1/tasks/$($task.taskId)"
Invoke-RestMethod -Method Delete -Uri "http://127.0.0.1:8080/api/v1/tasks/$($task.taskId)"
```

Default mode accepts QUEUED tasks and supports lookup/cancellation without teammate adapters. Core dispatch, lifecycle, deadlines, and aggregation are implemented, but production execution is deliberately disabled until approved AgentDirectory, AgentClient, and TaskExecutionEngine beans are supplied. Setting TASK_EXECUTION_ENABLED=true without all three fails startup. There is no production mock fallback.

With adapters enabled, matching agents execute concurrently through the execution-engine port. Tasks move through QUEUED/RUNNING to SUCCEEDED, FAILED, or CANCELLED. GET /api/v1/tasks/{taskId}/result returns terminal results grouped by agent ID, or a sanitized failure; pending results return 409. DELETE is idempotent for cancellation but returns 409 for already succeeded/failed tasks. Aggregation is all-or-nothing broadcast, not least-utilized-node load balancing.

Storage is process-local, lost at restart, and limited to 1000 records by default. Full admission reclaims terminal records older than TASK_RETENTION; queued/running records are never evicted. Exhaustion returns HTTP 503. Authentication, Redis integration, and production adapter verification remain pending.

Configuration: SERVER_ADDRESS (127.0.0.1), SERVER_PORT (8080), MAX_STORED_TASKS (1000, minimum 1), TASK_EXECUTION_ENABLED (false), TASK_TIMEOUT (30s), MAX_AGENTS_PER_TASK (16, range 1-64), TASK_RETENTION (1h). Default development API is local-only.

Run the full-context execution tests with explicitly test-only simulated agents:

```powershell
mvn -Dtest=OrchestrationApiIntegrationTest test
```

See [core module handoff](docs/CORE_MODULE_HANDOFF.md) for implementation evidence, result limits, and teammate responsibilities. These tests do not verify real network agents or Robin's production executor.

See [contracts](docs/INTEGRATION_CONTRACTS.md), [progress](docs/PROGRESS.md), and [test report](docs/TEST_REPORT.md). No 10,000+ connection target has been verified.

## Team Workflow

Develop on feature/, fix/, docs/, chore/, or test/ branches. Install local Git hooks once per clone:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/Install-GitHooks.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/Test-GitHooks.ps1
```

Use signed commits and PRs into main. See [Git workflow](docs/GIT_WORKFLOW.md) for signing, GitHub settings, and CI check names. [Week 1 acceptance](docs/WEEK1_ACCEPTANCE.md) is a historical project-planning audit, not an official individual weekly assignment or proof of current remote status.

This is a Team Project work under Zaalima Development Intership program . This particular project is about how to handle lots of work simultaneously and survive failures 
