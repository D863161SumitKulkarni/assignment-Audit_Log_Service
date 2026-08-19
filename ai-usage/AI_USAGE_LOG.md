# AI-Assisted Engineering Usage Log

## Purpose

This log records the AI-assisted work performed for the Audit Log Service assignment. It distinguishes requested work, AI-generated implementation, human decisions, corrections, and validation evidence.

## Session Context

- **Candidate:** Sumit Kulkarni
- **Assignment:** Build an AI-Assisted Software Engineering System - Audit Log Service
- **Start date:** 18/08/2026
- **Submission date:** 19/08/2026
- **Primary stack:** Java 21, Spring Boot 3, Maven, PostgreSQL, Jakarta Persistence, Lombok
- **Working style:** AI proposed or generated drafts; repository state, requirements, compile output, tests, and runtime behavior were checked before acceptance.

## Chronological Record

### 1. Submission attestation

- **Request:** Create a concise private-repository attestation with candidate, assignment, dates, and individual-work statement.
- **AI assistance:** Drafted `ATTESTATION.md` with corrected capitalization and professional formatting.
- **Human decision:** Accepted the concise metadata-plus-statement format.
- **Validation:** Read back the completed Markdown file.

### 2. Requirement analysis

- **Request:** Create `docs/01-requirement-analysis.md` covering objective, functional and non-functional requirements, assumptions, ambiguity handling, scope, acceptance criteria, and ownership.
- **AI assistance:** Converted the assignment brief into structured requirements and normalized obvious typos such as `resourceId`, `actorId`, and `resourceType`.
- **Human decision:** Accepted backend-only scope, server timestamps, SHA-256, append-only API, soft archival, redacted responses, and Swagger/OpenAPI review.
- **Validation:** Confirmed all requested sections and no diagnostics.

### 3. Task decomposition

- **Request:** Create `docs/02-task-decomposition.md` with 11 phases, dependencies, implementation sequencing, acceptance criteria, commit suggestions, AI notes, and human review gates.
- **AI assistance:** Produced the phase tables and cross-phase review protocol.
- **Human decision:** Accepted the dependency sequence and explicit AI/human ownership model.
- **Validation:** Checked all 11 phase headings and key testing/traceability terms.

### 4. Database schema

- **Request:** Create PostgreSQL DDL for `audit.event`, indexes, and tamper-evidence comments.
- **AI assistance:** Normalized malformed names and types to `UUID`, snake_case columns, `JSONB`, `TIMESTAMPTZ`, and `SHA-256` defaults.
- **Human decision:** Accepted the repository’s existing `database/schema.sql` filename instead of the prompt typo `schema.sat`.
- **Validation:** Workspace SQL diagnostics reported no errors.

### 5. Backend package structure

- **Request:** Create `config`, `controller`, `dto`, `entity`, `exception`, `repository`, `service`, and `util` packages.
- **AI assistance:** Created the requested directories under the actual Maven project path `backend/audit-log-service/src/main/java/com/auditlog`.
- **Human decision:** Accepted the existing Maven project nesting.
- **Validation:** Listed the resulting package tree.

### 6. Entity and DTOs

- **Request:** Create the `AuditEvent` JPA entity and six DTOs with Jakarta and Lombok annotations.
- **AI assistance:** Generated production-readable entity mappings, lifecycle defaults, JSONB fields, DTO builders, and validation annotations.
- **Human decision:** Accepted normalized Java field names such as `resourceId`, `payloadOriginal`, and `payloadRedacted`.
- **Validation:** Maven compilation passed.

### 7. Repository and specifications

- **Request:** Create `AuditEventRepository` with ordered lookup/export methods and dynamic specification support; create `AuditEventSpecifications` with filters and archive behavior.
- **AI assistance:** Generated Spring Data interfaces and Criteria API predicates.
- **Human decision:** Accepted `JpaSpecificationExecutor` and default exclusion of archived records for normal queries.
- **Validation:** Maven compilation and file diagnostics passed.

### 8. JSON and hashing utilities

- **Request:** Create deterministic JSON serialization and SHA-256 hash services.
- **AI assistance:** Initially generated Jackson 3 `tools.jackson` imports because the project was then configured with Spring Boot 4.1.0.
- **Correction:** After the POM was aligned to Spring Boot 3, compilation and editor diagnostics showed Jackson 2 was required. `JsonUtil` was updated to `com.fasterxml.jackson` imports and `ObjectMapper.copy()`.
- **Human decision:** Accepted stable map-key ordering, preserved nulls, UTF-8 hashing, lowercase hex, and pipe-delimited deterministic fields.
- **Validation:** Maven compilation passed after the compatibility correction.

### 9. Core services and controllers

- **Request:** Create mapper, append/query service, verification service/controller, retention service/controller, redaction service/controller, export service/controller, and compliance reporting service/controller.
- **AI assistance:** Generated the service and REST endpoint implementations from the requested contracts.
- **Human decisions:** Accepted server-assigned timestamps, genesis hash of 64 zeroes, original-payload verification after redaction, soft archival, export metadata hashing, and the Scenario C allowlist (`CLIENT_ACCOUNT` with four event types).
- **Correction:** Redaction missing-resource behavior was changed from `IllegalArgumentException` to `ResourceNotFoundException` to match the exception contract.
- **Validation:** Backend compilation passed after each implementation group.

### 10. Exception handling and tests

- **Request:** Add exception classes and focused JUnit 5/Mockito tests for hashing, append behavior, chain verification, and redaction.
- **AI assistance:** Generated the test classes and mock interactions.
- **Corrections:** Added missing JUnit imports, removed a raw `Map` captor, and made only shared redaction fixtures lenient under Mockito strict stubbing.
- **Human decision:** Accepted tests covering genesis links, latest-hash chaining, server timestamps, no delete calls, archived verification, original payload use, redaction metadata, and missing-event errors.
- **Validation:** 24 focused tests passed before the later security additions.

### 11. Maven and runtime debugging

- **Problem:** `mvnw.cmd clean install` failed because the POM mixed Spring Boot 4.1.0 starters with the documented Spring Boot 3 stack, had a missing Springdoc version, and the application used the Boot 4 `EntityScan` package.
- **AI assistance:** Inspected exact Maven errors, updated the POM to Spring Boot 3.5.16, standard Boot 3 starters, Springdoc `2.8.13`, and restored the Boot 3 `EntityScan` import.
- **Problem:** Swagger loaded but database-backed endpoints returned 500.
- **Diagnosis:** Direct JDBC confirmed `relation "audit.event" does not exist`.
- **Human decision:** Accepted `hibernate.hbm2ddl.create_namespaces=true` for the local prototype and documented migration/DDL limitations.
- **Validation:** A temporary instance on port 8081 returned HTTP 200 for `GET /api/audit/events` after schema creation. Later `mvnw.cmd clean install` completed with `BUILD SUCCESS`.

### 12. Security and edge-case review

- **Request:** Analyze the project, identify improvements, add security, run tests/edge cases, document results, and record the conversation.
- **AI assistance:** Reviewed all implementation surfaces and identified high-priority risks: missing authentication, malformed client inputs becoming 500s, unbounded pagination, committed database credentials, concurrent append risk, weak export coverage, and test gaps.
- **Accepted implementation:** Added Spring Security HTTP Basic, stateless sessions, `ADMIN` and `AUDITOR` roles, public Swagger/OpenAPI resources, environment-backed credentials, malformed-request 400 handling, and page-size bounds of 1-100.
- **Authorization decision:** Auditors may read/query/verify/export/compliance-report; only admins may append, redact, and archive.
- **Correction:** `ConstraintViolationException` was added to the 400 handler after the security integration test exposed a 500 for `size=101`.
- **Validation:** Added `SecurityIntegrationTest`; the final suite passed 30 tests with zero failures, errors, or skips.

### 13. Live Swagger and JSONB integrity diagnosis

- **Request:** Open Swagger, exercise the running API, and investigate remaining runtime behavior.
- **AI assistance:** Probed `http://localhost:8080/swagger-ui/index.html`, `/v3/api-docs`, secured audit endpoints, malformed query parameters, auditor/admin permissions, append, and verification.
- **Observed behavior:** Swagger/OpenAPI returned 200; unauthenticated audit access returned 401; auditor read returned 200; malformed inputs returned 400; auditor append returned 403; admin append returned 201. Verification returned HTTP 200 with `CURRENT_HASH_MISMATCH` for a smoke-test record.
- **Diagnosis:** PostgreSQL JSONB normalized whitespace in the stored payload. Verification was hashing the persisted representation instead of canonicalizing parsed JSON.
- **Accepted solution:** `ChainVerificationService` now parses `payloadOriginal` with `JsonUtil` and serializes it canonically before hash recalculation. The old inconsistent smoke row was not rewritten, because silently repairing it would conceal the integrity signal.
- **Validation:** Focused chain/security tests and the full Maven suite passed after the correction.

### 14. Architecture documentation

- **Request:** Create `docs/03-architecture-overview.md` covering system purpose, component diagram, package structure, data model, API groups, hash chain, redaction, retention, export, compliance reporting, decisions, trade-offs, and production hardening.
- **AI assistance:** Reviewed the implemented Java package tree, security design, controllers, services, repository, entity, and PostgreSQL architecture, then drafted a Markdown overview aligned with the actual prototype.
- **Human decision:** Accepted the architecture document with explicit limitations for concurrent appends, database-level append-only enforcement, export completeness, and production identity management.
- **Artifact:** `docs/03-architecture-overview.md`
- **Validation:** Markdown diagnostics reported no errors.

### 17. Concurrent append serialization

- **Date:** 19/08/2026
- **Request:** Begin fixing the first identified high-priority problem: concurrent appends can read the same latest hash and fork the chain.
- **Context reviewed:** `AuditEventService`, `AuditEventRepository`, append unit tests, the problem register, and PostgreSQL transaction behavior.
- **AI solution provided:** Added a transaction-scoped PostgreSQL advisory lock through `JdbcTemplate`, acquired before latest-hash lookup and event persistence. This works across application instances sharing the database and does not rely on a JVM-local lock.
- **Human decision:** Accepted advisory locking as the first prototype fix, while keeping the issue in progress until a real PostgreSQL concurrency integration test is added.
- **Artifacts:** `AuditEventService.java`, `AuditEventServiceTest.java`, `docs/identified-problems-and-solutions.md`, `docs/03-architecture-overview.md`, and `docs/10-risks-tradeoffs-limitations.md`.
- **Validation:** Focused append tests and full project compilation passed. Remaining validation is a concurrent PostgreSQL integration test covering chain continuity and rollback.

### 18. Compliance reporting correctness

- **Date:** 19/08/2026
- **Request:** Continue execution by fixing the next identified problems after append serialization.
- **Context reviewed:** `ComplianceReportService`, `ComplianceReportController`, compliance DTO/API documentation, testing strategy, and the identified-problems register.
- **AI solution provided:** Added an explicit `includeArchived` query parameter defaulting to `false`, preserved the option to include archived records for historical reporting, forced deterministic ascending `id` ordering, and changed `totalRecords` to use the repository page's total matching count.
- **Human decision:** Accepted the explicit archive-scope contract and full-count semantics as the clearer compliance API behavior.
- **Artifacts:** `ComplianceReportService.java`, `ComplianceReportController.java`, `ComplianceReportServiceTest.java`, `docs/04-api-design.md`, `docs/08-scenario-c-compliance-reporting.md`, `docs/13-test-results-and-improvements.md`, and `docs/identified-problems-and-solutions.md`.
- **Validation:** Focused compliance tests passed. The subsequent full suite passed 32 tests with zero failures, errors, or skips.

### 19. Correctness and coverage improvements

- **Date:** 19/08/2026
- **Request:** Resolve the practical issues that could prevent the submission from reaching a 4/5 rating.
- **Context reviewed:** The problem register, redaction/export/compliance services, API documentation, schema, test inventory, and live-defense evidence.
- **AI solution provided:** Implemented cumulative and dotted-path nested redaction with deep mutable copies; strengthened export hashes with every returned record's current hash; bounded exports at 10,000 records; rejected reversed time ranges; added a resource-ID index and schema checks; removed empty misspelled duplicate documents; populated live-defense notes; and added export, compliance, retention, nested-redaction, repeated-redaction, and time-range tests.
- **Human decision:** Accepted these as prototype improvements while retaining explicit production limitations for external anchoring, database-level immutability, migrations, identity, TLS, and scale.
- **Artifacts:** Updated services, tests, schema, README, API/Scenario B documentation, risk register, test results, and `docs/11-live-defence-notes.md`.
- **Validation:** Focused tests passed; the latest full-suite baseline is 41 tests with zero failures, errors, or skips after final rerun.

### 20. Security property binding and retention coverage

- **Date:** 19/08/2026
- **Request:** Continue resolving issues that could keep the assessment below 4/5.
- **AI solution provided:** Added typed `SecurityProperties` binding and configuration-processor support for custom security settings, added retention service regression tests, and removed remaining raw generic test matchers.
- **Human decision:** Accepted the typed configuration approach and retained environment-backed local defaults as a documented prototype boundary.
- **Artifacts:** `SecurityProperties.java`, `SecurityConfig.java`, `pom.xml`, `RetentionServiceTest.java`, and the problem/test documentation.
- **Validation:** Clean Maven tests pass; the final repository baseline remains 41 tests with zero failures, errors, or skips.

### 15. Ongoing AI traceability

- **Request:** Continue updating the AI usage log as the conversation continues.
- **Accepted process:** Future AI-assisted changes should record the date, user request, context reviewed, solution provided, human acceptance or correction, artifact paths, and validation evidence in this file.
- **Ownership:** The candidate remains responsible for the accuracy of this log and must correct any entry that does not reflect the actual work performed.

### 16. Hash-chain design documentation

- **Date:** 19/08/2026
- **Request:** Create `docs/05-hash-chain-design.md` explaining the tamper-evident chain, hash fields, previous-link behavior, genesis value, SHA-256 rationale, server timestamps, verification algorithm, violation types, tamper detection, limitations, and archived/redacted record behavior.
- **Context reviewed:** `HashService`, `ChainVerificationService`, `JsonUtil`, the audit entity, the PostgreSQL schema, and the existing architecture/API documents.
- **AI solution provided:** Created a Markdown design document describing the actual delimiter-separated hash input, 64-zero genesis hash, JSONB re-canonicalization during verification, first-failure reporting, `PREVIOUS_HASH_MISMATCH`, `CURRENT_HASH_MISMATCH`, and production options including external anchoring, signatures, WORM storage, and immutable ledgers.
- **Human decision:** Accepted the document as a prototype-level integrity design while retaining the stated limitations around malicious database administrators, concurrent appends, delimiter ambiguity, and lack of external notarization.
- **Artifact:** `docs/05-hash-chain-design.md`
- **Validation:** Markdown diagnostics reported no errors.

## Accepted Artifacts

- Requirements and task decomposition documentation.
- PostgreSQL schema and tamper-test SQL.
- Java entity, DTO, repository, specification, utility, service, controller, and exception classes.
- Focused unit tests and security integration tests.
- Spring Security configuration and environment-backed application properties.
- Security design and test/improvement review documentation.

## Rejected or Corrected Approaches

- Jackson 3 imports were rejected after the project moved to Spring Boot 3; Jackson 2 imports were used instead.
- The Boot 4 `EntityScan` import was rejected because the assignment and final POM use Boot 3.
- The initial missing-event `IllegalArgumentException` was rejected in favor of `ResourceNotFoundException`.
- Shared strict Mockito stubs and a raw generic captor were corrected rather than suppressing test quality warnings broadly.
- Auditor access to POST `/api/audit/events` was rejected; writes are admin-only.

## Remaining Human-Owned Risks

The candidate remains responsible for reviewing and addressing concurrent append serialization, database-level append-only enforcement, nested/cumulative redaction semantics, stronger export integrity, migration strategy, external identity, TLS, tenant authorization, and production secret management. These are documented as remaining risks rather than claimed as solved by AI-generated code.

## Attestation

This log records the AI assistance and subsequent human review, correction, testing, and acceptance decisions honestly. The candidate owns the final requirements interpretation, source changes, configuration, tests, and submission.
