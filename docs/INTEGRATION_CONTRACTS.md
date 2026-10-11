# Integration Contracts

Status: Initial draft. Contracts are not final until reviewed with teammate modules.

## Health API

### `GET /api/v1/health`

Response `200 OK`:

```json
{
  "status": "UP",
  "service": "High-Concurrency Autonomous Agent Orchestrator",
  "version": "0.0.1-SNAPSHOT",
  "timestamp": "2026-10-11T00:00:00Z"
}
```

## Planned Java Interfaces

These interfaces will be added before core orchestration begins:

- `AgentDirectory`: discover registered and available agents.
- `AgentClient`: send execution requests to an agent and receive results.
- `TaskExecutionEngine`: execute orchestration work using the teammate-owned concurrency engine.
- `CoordinationLock`: acquire and release distributed locks through the teammate-owned Redis module.
- `TaskEventPublisher`: publish lifecycle events through Redis Pub/Sub.

Mocks or in-memory implementations used for HARI's module tests must be marked as temporary and must not be treated as production teammate implementations.
