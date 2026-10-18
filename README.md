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

Current APIs accept QUEUED tasks and support lookup/cancellation. Execution, aggregation, authentication, and Redis integration are pending. Storage is process-local, lost at restart, and limited to 1000 records by default; cancelled records still occupy capacity. Exhaustion returns HTTP 503.

Configuration: SERVER_ADDRESS (127.0.0.1), SERVER_PORT (8080), MAX_STORED_TASKS (1000, minimum 1). Default development API is local-only.

See [contracts](docs/INTEGRATION_CONTRACTS.md), [progress](docs/PROGRESS.md), and [test report](docs/TEST_REPORT.md). No 10,000+ connection target has been verified.

## Team Workflow

Develop on feature/, fix/, docs/, chore/, or test/ branches. Install local Git hooks once per clone:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/Install-GitHooks.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/Test-GitHooks.ps1
```

Use signed commits and PRs into main. See [Git workflow](docs/GIT_WORKFLOW.md) for signing, required GitHub settings, and CI check names. [Week 1 acceptance](docs/WEEK1_ACCEPTANCE.md) distinguishes completed local deliverables from remaining remote/team verification.

This is a Team Project work under Zaalima Development Intership program . This particular project is about how to handle lots of work simultaneously and survive failures 
