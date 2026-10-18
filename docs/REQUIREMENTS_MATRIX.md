# Requirements Matrix

| Requirement | Assigned Member | Status | Evidence |
| --- | --- | --- | --- |
| Verify workspace and toolchain | HARI | COMPLETED | Java 21.0.12.1, Maven 3.9.16, Git 2.52.0 checked on 2026-10-11 |
| Extract Project 1 PDF requirements | HARI | COMPLETED | Extracted with Apache PDFBox on 2026-10-11 |
| Create Spring Boot 3 backend | HARI | COMPLETED | `pom.xml`, `OrchestratorApplication` |
| Enable Java 21 Virtual Threads web configuration | HARI | COMPLETED | Real HTTP test asserts Thread.currentThread().isVirtual(); Robin's production engine is separate |
| Implement health endpoint | HARI | COMPLETED | `GET /api/v1/health`, `HealthControllerTest` |
| Implement core REST APIs | HARI | IN PROGRESS | POST/GET/DELETE task APIs verified; result API pending |
| Implement request/response DTOs | HARI | IN PROGRESS | Health and task DTOs verified; result DTOs pending |
| Implement task submission validation | HARI | COMPLETED | HTTP tests cover invalid capability, null/oversized payload, blank key, malformed JSON/UUID |
| Implement core orchestration service | HARI | IN PROGRESS | TaskService handles admission/lookup/cancel; dispatch/aggregation pending |
| Implement task submission and status handling | HARI | COMPLETED | TaskApiIntegrationTest verifies POST 202, Location, GET, and unknown IDs |
| Implement task lifecycle management | HARI | IN PROGRESS | QUEUED/CANCELLED implemented; execution states pending |
| Implement result aggregation | HARI | NOT STARTED | Pending design |
| Define core integration interfaces | HARI | COMPLETED | Discovery/client/execution/lease/event ports and schema documented; adapter implementation/team agreement remain separate |
| Implement centralized exception handling for current APIs | HARI | COMPLETED | ApiExceptionHandler and HTTP 400/404/503 tests |
| Implement agent registration and communication | Ahamed | WAITING FOR TEAMMATE | 3eca18d adds in-memory registry; 12 tests pass; secure REST/gRPC design and communication remain pending |
| Implement Virtual Thread execution engine | Robin | WAITING FOR TEAMMATE | Interface contract pending |
| Implement Structured Concurrency | Robin | WAITING FOR TEAMMATE | Java 21 preview configuration pending |
| Implement Redis Pub/Sub | Navitha | WAITING FOR TEAMMATE | Interface contract pending |
| Implement distributed locking | Navitha | WAITING FOR TEAMMATE | Interface contract pending |
| Validate 10,000+ concurrent connection target | UNASSIGNED | UNASSIGNED | Requires benchmark plan and measured result |
| Implement least-utilized-node load balancing | UNASSIGNED | UNASSIGNED | Not assigned |
| Integrate Resilience4j circuit breakers | UNASSIGNED | UNASSIGNED | Not assigned |
| Integrate Resilience4j bulkheads | UNASSIGNED | UNASSIGNED | Not assigned |
| Test Virtual Thread pinning behavior | UNASSIGNED | UNASSIGNED | Not assigned |
| Investigate memory leaks and resource usage | UNASSIGNED | UNASSIGNED | Not assigned |
| Prepare Docker Compose deployment | UNASSIGNED | UNASSIGNED | Not assigned |
| Benchmark Virtual Threads vs platform threads | UNASSIGNED | UNASSIGNED | Not assigned |
| Develop live terminal dashboard | UNASSIGNED | UNASSIGNED | Not assigned |
| Complete final documentation | HARI / Team | IN PROGRESS | Initial docs created |
| Create GitHub repository | HARI / Team | COMPLETED | Existing origin and October 11/13 commits verified |
| Push daily signed commits | HARI / Team | IN PROGRESS | Local signing configured; 13a2e75 unknown_key on GitHub; historical daily gaps remain |
| Deliver high-concurrency benchmarking report | UNASSIGNED | UNASSIGNED | Final deliverable from official PDF |
| Deliver final architectural evaluation roadmap | UNASSIGNED | UNASSIGNED | Final review requirement from official PDF |
| Restore full application startup | HARI | COMPLETED | Duplicate controller removed; full-context HTTP tests pass |
| Bound process-local task storage | HARI | COMPLETED | 60 attempts: exactly 10 admitted at capacity 10; HTTP 503 verified |
| Define branch/signing protocol and local hooks | HARI | COMPLETED | GIT_WORKFLOW.md; versioned hooks; 9 isolated hook regression checks pass |
| Enforce PR/signature/check rules on GitHub main | HARI / Team | IN PROGRESS | main protected=false at October 18 audit; CI supplied, remote run/settings pending |
| Deliver Week 1 acceptance evidence | HARI | COMPLETED | WEEK1_ACCEPTANCE.md distinguishes local deliverables and remote/team dependencies |
