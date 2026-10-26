# Requirements Matrix

Current core update: 2026-10-26. COMPLETED for HARI's algorithm/API rows means implemented and verified with labeled test adapters, not integrated production transport/executor/Redis. Official project weeks do not prescribe individual weekly assignments. See CORE_MODULE_HANDOFF.md and TEST_REPORT.md for verification boundaries.

| Requirement | Assigned Member | Status | Evidence |
| --- | --- | --- | --- |
| Verify workspace and toolchain | HARI | COMPLETED | Java 21.0.12.1, Maven 3.9.16, Git 2.52.0 checked on 2026-10-11 |
| Extract Project 1 PDF requirements | HARI | COMPLETED | Extracted with Apache PDFBox on 2026-10-11 |
| Create Spring Boot 3 backend | HARI | COMPLETED | `pom.xml`, `OrchestratorApplication` |
| Enable Java 21 Virtual Threads web configuration | HARI | COMPLETED | Real HTTP test asserts Thread.currentThread().isVirtual(); Robin's production engine is separate |
| Implement health endpoint | HARI | COMPLETED | `GET /api/v1/health`, `HealthControllerTest` |
| Implement core REST APIs | HARI | COMPLETED | Submission/status/cancellation/result endpoints; real HTTP execution tests |
| Implement request/response DTOs | HARI | COMPLETED | Health, task, and terminal result/failure DTOs |
| Implement task submission validation | HARI | COMPLETED | HTTP tests cover invalid capability, null/oversized payload, blank key, malformed JSON/UUID |
| Implement core orchestration service | HARI | COMPLETED | Dispatch through ports, candidate validation, deadlines, failure/cancellation; production adapters pending |
| Implement task submission and status handling | HARI | COMPLETED | TaskApiIntegrationTest verifies POST 202, Location, GET, and unknown IDs |
| Implement task lifecycle management | HARI | COMPLETED | QUEUED/RUNNING/SUCCEEDED/FAILED/CANCELLED; atomic terminal guards and late-result tests |
| Implement result aggregation | HARI | COMPLETED | Immutable agent-keyed output, all-or-nothing completion, correlation/size checks; tested doubles, not production agents |
| Define core integration interfaces | HARI | COMPLETED | Discovery/client/execution/lease/event ports and schema documented; adapter implementation/team agreement remain separate |
| Implement centralized exception handling for current APIs | HARI | COMPLETED | HTTP 400/404/409/503 and sanitized task-failure responses |
| Verify owned module with production teammate adapters | HARI / Team | WAITING FOR TEAMMATE | Core is ready for integration; real directory/client/executor and Redis semantics still need owner delivery and joint tests |
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
| Push daily signed commits | HARI / Team | IN PROGRESS | Local signing configured; a590731 verified by GitHub at latest audit; historical daily gaps remain; new commit user-controlled |
| Deliver high-concurrency benchmarking report | UNASSIGNED | UNASSIGNED | Final deliverable from official PDF |
| Deliver final architectural evaluation roadmap | UNASSIGNED | UNASSIGNED | Final review requirement from official PDF |
| Restore full application startup | HARI | COMPLETED | Duplicate controller removed; full-context HTTP tests pass |
| Bound process-local task storage | HARI | COMPLETED | Concurrent admission/HTTP 503 verified; expired terminal retention cleanup frees slots without evicting active tasks |
| Define branch/signing protocol and local hooks | HARI | COMPLETED | GIT_WORKFLOW.md; versioned hooks; 9 isolated hook regression checks pass |
| Enforce PR/signature/check rules on GitHub main | HARI / Team | IN PROGRESS | main protected=false at October 18 audit; CI supplied, remote run/settings pending |
| Deliver Week 1 acceptance evidence | HARI | COMPLETED | WEEK1_ACCEPTANCE.md distinguishes local deliverables and remote/team dependencies |
