# Architecture

## Current System Shape

Client or Postman sends HTTP requests to Spring Boot REST controllers. Controllers validate input and delegate to application services. The orchestration service will coordinate task lifecycle, select agents through an agent gateway interface, execute work through a concurrency engine interface, and aggregate results.

## Package Direction

- `com.zaalima.orchestrator.api`: REST controllers.
- `com.zaalima.orchestrator.api.dto`: external request and response DTOs.
- `com.zaalima.orchestrator.config`: application configuration.
- `com.zaalima.orchestrator.core`: orchestration domain and service logic, to be added.
- `com.zaalima.orchestrator.integration`: interfaces for teammate-owned modules, to be added.

## Implemented Flow

`GET /api/v1/health`

1. `HealthController` receives the request.
2. It returns service name, version, status, and timestamp.

## Planned Task Flow

1. Client submits a task request.
2. REST controller validates request body.
3. Orchestration service creates a task record and lifecycle state.
4. Orchestration service asks the agent registry/communication interface for available agents.
5. Orchestration service delegates execution through the concurrency engine interface.
6. Result aggregation updates task status and response data.
7. Client polls task status or receives final response depending on API design.

## Design Constraints

- Keep orchestration independent from Redis, gRPC, and concrete thread execution details.
- Use dependency injection for integration boundaries.
- Label test doubles and mocks clearly.
