# Final Engineering Summary

## 1. Executive Summary

This submission delivers a backend-only, tamper-evident audit log service built as an AI-assisted engineering exercise. The service accepts append-only audit events, stores them in PostgreSQL, links them with a SHA-256 hash chain, supports filtered queries and pagination, verifies the first chain inconsistency, and extends the core flow with retention, redaction, export, and clarified compliance reporting.

The implementation is intentionally presented as a reviewable prototype. It demonstrates engineering judgment, requirement clarification, controlled AI use, security boundaries, automated testing, manual validation, and explicit production limitations.

## 2. Assignment Interpretation

The primary assignment objective was interpreted as building a working API-driven audit service rather than a frontend application. The system is validated through APIs, Swagger/OpenAPI, PostgreSQL inspection, tests, and a direct tamper demonstration.

The chosen timestamp policy is server-assigned `Instant.now()` time. This prevents callers from backdating events and makes the service timestamp part of the integrity commitment.

Scenario C was normalized from the ambiguous statement about regulators auditing client account data into a report over:

- `resourceType=CLIENT_ACCOUNT`
- `eventType` in `ACCOUNT_VIEWED`, `ACCOUNT_EXPORTED`, `ACCOUNT_UPDATED`, and `PERMISSION_GRANTED`
- Optional client-account, actor, timestamp, pagination, and archive-scope filters

This interpretation is documented as an assignment-level prototype boundary, not as legal compliance certification.

## 3. Delivered Artifacts

### Application

- Java 21 Spring Boot 3.5.16 Maven project.
- PostgreSQL/Jakarta Persistence entity for `audit.event`.
- Lombok DTOs and request validation.
- Spring Data JPA repository and dynamic specifications.
- Deterministic Jackson JSON utility.
- SHA-256 hashing service.
- Append/query service with PostgreSQL transaction-scoped advisory locking.
- Chain verification service and controller.
- Retention archival service and controller.
- Cumulative dotted-path redaction service and controller.
- Actor/resource export service and controller.
- Scenario C compliance reporting service and controller.
- Global API exception handling.
- Stateless HTTP Basic security with `ADMIN` and `AUDITOR` roles.

### Database

- PostgreSQL `audit.event` schema and indexes.
- Resource ID export index.
- Hash, algorithm, archive-state, and redaction-state checks.
- Tamper demonstration script at [database/tamper-test.sql](../database/tamper-test.sql).

### Documentation

- Requirements and assumptions.
- Task decomposition and dependencies.
- Architecture overview.
- API design.
- Hash-chain design.
- Scenario A validation.
- Scenario B retention, redaction, and export.
- Scenario C compliance analysis.
- Testing strategy.
- Risks, trade-offs, and limitations.
- Security design.
- Test results and improvement review.
- Live-defense notes.
- Identified problems and solutions.
- AI usage traceability.
- Root README and attestation.

## 4. Key Design Decisions

| Decision | Rationale |
| --- | --- |
| Backend API only | The assignment is validated through APIs and does not require an external consumer. |
| PostgreSQL durable store | Provides transactional persistence, JSONB support, indexing, and direct tamper-test access. |
| Server-assigned timestamp | Prevents caller backdating and creates a consistent service-side time boundary. |
| SHA-256 hash chain | Deterministic, widely supported, easy to reproduce, and suitable for prototype tamper evidence. |
| 64-zero genesis hash | Defines a stable first-record chain anchor. |
| Transaction-scoped PostgreSQL advisory lock | Serializes append operations across application instances sharing the same database. |
| Original payload as integrity source | Redaction changes responses without invalidating the original hash commitment. |
| Soft archival | Preserves records and chain verification while allowing active queries to exclude old data. |
| Explicit compliance archive scope | Excludes archived reports by default while permitting historical inclusion. |
| Stateless HTTP Basic prototype security | Small, reviewable role separation for local Swagger/API validation. |

## 5. Validation Evidence

### Automated tests

The final verified Maven test baseline is:

```text
41 tests
0 failures
0 errors
0 skipped
```

Coverage includes:

- Hash determinism and null handling.
- Genesis and previous-hash linkage.
- Append-only creation and server timestamps.
- PostgreSQL advisory-lock invocation.
- Chain success, previous-link failure, current-hash failure, archived verification, and original-payload verification.
- Cumulative and nested redaction.
- Retention archival and integrity-field preservation.
- Compliance ordering, archive scope, total matching count, and invalid time range.
- Export record-hash commitment and export-size bound.
- Security authentication, roles, malformed timestamps, and pagination limits.
- Application context and repository wiring.

### Build quality gates

The following command completed successfully:

```powershell
$env:DB_PASSWORD = "<local-postgres-password>"
.\mvnw.cmd clean install
```

### Manual API validation

Live Swagger/API checks verified:

- Swagger UI: HTTP 200.
- OpenAPI JSON: HTTP 200.
- Unauthenticated audit query: HTTP 401.
- Auditor read query: HTTP 200.
- Auditor append attempt: HTTP 403.
- Admin append: HTTP 201.
- Malformed timestamp: HTTP 400.
- Page size above 100: HTTP 400.
- Verification endpoint: HTTP 200 with structured integrity result.

### Tamper validation

The PostgreSQL tamper script changes `payload_original` without changing `current_hash`. The expected verification result is:

```json
{
  "chainIntact": false,
  "violationType": "CURRENT_HASH_MISMATCH"
}
```

The intentionally inconsistent local smoke row was not silently repaired because doing so would hide the tamper-evidence result. A clean database should be recreated before the final demonstration, followed by a fresh append, intact verification, tampering, and failed verification.

## 6. Problems Resolved During Review

The review identified and addressed the following practical blockers:

- Spring Boot 4/Boot 3 dependency mismatch.
- Missing Springdoc dependency version.
- Boot 4 `EntityScan` import under a Boot 3 POM.
- Missing PostgreSQL `audit` namespace/table at runtime.
- Jackson 3 imports under Spring Boot 3/Jackson 2.
- Missing repository component scanning.
- Unauthenticated audit endpoints.
- Auditor mutation access.
- Malformed client inputs incorrectly returning 500.
- Unbounded page sizes.
- Compliance archived-record scope ambiguity.
- Compliance nondeterministic ordering.
- Compliance page-count versus total-count confusion.
- Repeated redaction re-exposing fields.
- Nested redaction failures caused by immutable nested maps.
- Weak export commitment coverage.
- Unbounded export result size.
- Missing resource export index.
- Reversed time ranges silently returning empty results.
- Missing retention, export, compliance, nested-redaction, repeated-redaction, and time-range tests.
- Empty duplicate misspelled documentation files.
- Empty live-defense notes.

The current state of each remaining issue is tracked in [identified-problems-and-solutions.md](identified-problems-and-solutions.md).

## 7. Assumptions

- PostgreSQL is the system of record.
- A single logical chain is sufficient for this assignment.
- The application/server clock is trusted for event timestamps.
- Payloads are structured JSON objects.
- Archived records remain available for verification.
- The selected Scenario C event allowlist represents access for this prototype.
- Authentication and authorization requirements beyond prototype roles are not defined by the assignment.
- Legal retention, legal hold, regulator identity, and regulator-specific output formats require stakeholder clarification.

## 8. Remaining Limitations

The following limitations are intentional and documented:

- A fully privileged database administrator can potentially rewrite all records, hashes, or verification code.
- Tail deletion can remain undetected without an independent chain-head checkpoint.
- Database-level append-only enforcement is not yet implemented through restricted roles, triggers, or WORM storage.
- The advisory lock has unit coverage, but a real multi-threaded PostgreSQL concurrency integration test remains recommended.
- External chain anchoring, digital signatures, WORM storage, and immutable-ledger integration are not implemented.
- Original sensitive payload data remains stored internally after response redaction.
- Export filtering does not prove global completeness outside the selected scope.
- Large-scale streaming export and full-chain streaming verification are future work.
- `ddl-auto=update` is a local prototype convenience; production should use versioned migrations.
- HTTP Basic with in-memory users is not a production identity platform.
- TLS, MFA, tenant isolation, rate limiting, and security monitoring require deployment-level controls.
- Spring Data `PageImpl` response serialization should be replaced by a stable page DTO before external client integration.

These limitations are not hidden defects; they are the stated boundary between an assignment prototype and a production audit ledger.

## 9. AI-Assisted Engineering Ownership

AI assistance was used for requirement decomposition, code and test drafting, debugging, documentation, security review, and live validation planning. Human review determined which outputs were accepted, modified, rejected, or validated.

The candidate owns:

- Requirements interpretation.
- Architecture and technology decisions.
- Source-code correctness.
- Security and privacy decisions.
- Test adequacy.
- Runtime and database validation.
- Accuracy of all documentation and AI usage records.

The complete chronological trace is maintained in [ai-usage/AI_USAGE_LOG.md](../ai-usage/AI_USAGE_LOG.md).

## 10. Submission Readiness

Before final submission:

- Run `mvnw.cmd clean install` from `backend/audit-log-service`.
- Recreate a clean local database.
- Capture Swagger, append, query, intact verification, tamper, and failed-verification screenshots.
- Confirm `docs/11-live-defence-notes.md` is rehearsed.
- Confirm the AI usage log reflects the final changes.
- Confirm the private GitHub repository contains the full development history and no committed secrets.

## Final Assessment

The repository is a strong, defensible prototype for the assignment. It demonstrates the requested API behavior, scenario extensions, integrity checks, security controls, automated validation, AI traceability, and engineering judgment. The remaining gaps are primarily production-hardening concerns and final evidence capture rather than missing core assignment functionality.
