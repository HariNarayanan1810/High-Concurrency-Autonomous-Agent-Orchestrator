# Requirements Matrix

| Requirement | Assigned Member | Status | Evidence |
| --- | --- | --- | --- |
| Verify workspace and toolchain | HARI | COMPLETED | Java 21.0.12.1, Maven 3.9.16, Git 2.52.0 checked on 2026-10-11 |
| Extract Project 1 PDF requirements | HARI | COMPLETED | Extracted with Apache PDFBox on 2026-10-11 |
| Create Spring Boot 3 backend | HARI | COMPLETED | `pom.xml`, `OrchestratorApplication` |
| Enable Java 21 Virtual Threads configuration | HARI / Robin integration | IN PROGRESS | `spring.threads.virtual.enabled=true`; production execution engine belongs to Robin |
| Implement health endpoint | HARI | COMPLETED | `GET /api/v1/health`, `HealthControllerTest` |
| Implement core REST APIs | HARI | NOT STARTED | Pending task APIs |
| Implement request/response DTOs | HARI | IN PROGRESS | Health DTO only |
| Implement request validation | HARI | NOT STARTED | Pending task APIs |
| Implement core orchestration service | HARI | NOT STARTED | Pending design |
| Implement task submission and status handling | HARI | NOT STARTED | Pending design |
| Implement task lifecycle management | HARI | NOT STARTED | Pending design |
| Implement result aggregation | HARI | NOT STARTED | Pending design |
| Define integration interfaces | HARI | NOT STARTED | Pending contracts |
| Implement centralized exception handling | HARI | NOT STARTED | Pending APIs |
| Implement agent registration and communication | Ahamed | WAITING FOR TEAMMATE | Interface contract pending |
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
| Create GitHub repository | HARI / Team | NOT STARTED | Requires user approval before remote setup |
| Push daily signed commits | HARI / Team | NOT STARTED | Requires GitHub repo, commit signing setup, and hooks |
| Deliver high-concurrency benchmarking report | UNASSIGNED | UNASSIGNED | Final deliverable from official PDF |
| Deliver final architectural evaluation roadmap | UNASSIGNED | UNASSIGNED | Final review requirement from official PDF |
