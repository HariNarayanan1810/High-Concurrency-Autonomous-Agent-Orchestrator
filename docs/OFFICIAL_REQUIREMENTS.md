# Official Requirements

Source extracted from: `C:\IMP\Resume\Zaalima Development\Java Development Project Document.pdf`

Status: COMPLETED for Project 1 extraction on 2026-10-11 using Apache PDFBox.

## Project 1 Scope

- Build a Java 21+ / Spring Boot 3 backend for a High-Concurrency Autonomous Agent Orchestrator.
- Do not implement Project 2.
- Solve the bottleneck of traditional Java thread pools when handling thousands of simultaneous I/O-bound AI agent workflows.
- Use Java 21 Virtual Threads and Structured Concurrency to execute massive autonomous microservice requests without exhausting operating system resources.
- Support task submission, orchestration, agent coordination, concurrent execution, result aggregation, and task status visibility.

## Week 1: Backend Foundation

- Establish the Spring Boot 3 backend.
- Use Java 21 Virtual Threads for lightweight concurrent execution.
- Establish Zaalima Development Pvt. Ltd. internship branch management protocols.
- Require strict GitHub commit signatures for all backend modules.
- Design gRPC and REST communication interfaces for agents to securely register with the orchestrator.
- Establish clearly defined module interfaces.

## Week 2: Concurrency and Distributed Coordination

- Implement Structured Concurrency using the appropriate Java 21 preview APIs.
- Implement Redis Pub/Sub messaging.
- Implement distributed locking.
- Coordinate execution across components.
- Submit foundational concurrency models for the initial peer review cycle.
- Address the specified target of 10,000+ concurrent connections.
- Validate GitHub hook enforcement and Redis distributed locking for the mid-project review.

## Week 3: Load Balancing and Resilience

- Implement least-utilized-node load balancing.
- Integrate Resilience4j circuit breakers.
- Integrate Resilience4j bulkheads.
- Test concurrency-related issues.
- Test Virtual Thread pinning behavior.
- Investigate memory leaks and resource usage.

## Week 4: Deployment, Benchmarking, and Final Delivery

- Prepare Docker Compose deployment.
- Benchmark Virtual Threads against traditional platform threads.
- Develop a live terminal dashboard showing thread execution information.
- Complete documentation.
- Run integration tests.
- Prepare the final demonstration and submission.
- Finalize the operational structure and scoring matrices for the Zaalima Development Pvt. Ltd. evaluation roadmap.
- Deliver a high-concurrency orchestrator benchmarking report.

## Acceptance Rules

- Do not mark requirements COMPLETED until implemented and verified.
- Do not claim the 10,000+ concurrent connection target without measured evidence.
- Distinguish mocked integrations from production integrations.
- Record actual test commands and results.
- Create a GitHub repository before development work is treated as officially started.
- Push commits daily during every project week.
- Use professional commit messages, logical branch management, and standard collaborative workflows.
- Do not claim commit signatures or hook enforcement until actually configured and verified.

## Ambiguities To Verify

- Exact agent registration security requirements.
- Whether gRPC is mandatory or optional for agent communication.
- Exact GitHub hook setup and signed commit mechanism.
- Required benchmark tooling and acceptance threshold.
- Exact terminal dashboard requirements.
