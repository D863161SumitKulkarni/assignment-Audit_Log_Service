# Identified Problems and Solutions

## Purpose

This is a living engineering checklist for problems identified during assignment review. Each issue includes its impact, proposed solution, current status, and the evidence required before it can be marked complete.

Status values:

- **Open:** identified but not fixed.
- **In progress:** implementation or validation is underway.
- **Resolved:** implemented and validated.
- **Accepted limitation:** intentionally not implemented for the prototype and documented as a boundary.

## Problem Register

| ID | Area | Problem | Impact | Proposed solution | Status |
| --- | --- | --- | --- | --- | --- |
| P-01 | Hash-chain writes | Concurrent appends can read the same latest hash and create a fork. | The linear chain may no longer represent one ordered history. | Serialize appends with a PostgreSQL advisory lock, locked chain-head row, or serializable transaction with retry; add a concurrency integration test. | In progress |
| P-02 | Tamper detection | Deleting the tail of the chain can leave the remaining prefix internally valid. | The service cannot detect missing final records without an independent expected chain head. | Store and protect a chain-head checkpoint, externally anchor periodic hashes, or narrow the claim to detecting changes in retained records and broken internal links. | Open |
| P-03 | Database immutability | The public API is append-only, but privileged SQL users can still update or delete rows. | API-level immutability does not provide database-level tamper prevention. | Add restricted PostgreSQL roles, immutable triggers, write-only procedures, or WORM/immutable storage. | Open |
| P-04 | Compliance archive scope | Compliance reporting previously excluded archived records unconditionally, while historical reporting may require them. | Reports could omit legitimate historical access events after retention archival. | Add explicit `includeArchived` control, default false, and document/test the historical-reporting behavior. | Resolved |
| P-05 | Compliance ordering | Compliance queries previously did not explicitly request ascending ID ordering. | Result order could be nondeterministic, weakening reproducibility. | Use `Sort.by(Direction.ASC, "id")` in the compliance pageable and add an ordering test. | Resolved |
| P-06 | Compliance count | `totalRecords` previously represented the current page size rather than all matching records. | Consumers could misread a page count as the total report count. | Use `Page.getTotalElements()` and test a multi-page result. | Resolved |
| P-07 | Redaction repeatability | Each redaction starts from `payloadOriginal`; a later request can re-expose fields removed earlier. | Sensitive data may return to the API response after repeated redaction. | Apply new redaction rules to the existing redacted representation or persist a cumulative redaction policy; add repeated-redaction tests. | Resolved |
| P-08 | Redaction depth | Redaction currently removes top-level fields only. | Nested sensitive values may remain exposed. | Define JSON path semantics and implement recursive/path-aware redaction; add nested-field tests. | Resolved |
| P-09 | Original sensitive data | `payloadOriginal` remains stored internally after API redaction. | A privileged database or backup operator can still access sensitive data. | Encrypt sensitive fields, use managed key destruction/cryptographic erasure, or store cryptographic commitments instead of plaintext where policy permits. | Accepted limitation |
| P-10 | Export integrity | Export hash covers metadata, ordered event IDs, and first/last chain boundaries, but not every middle record's full commitment. | A changed middle record may not change the export hash. | Include every record's committed hash/canonical content or a Merkle-style root and define independent verification steps. | Resolved |
| P-11 | Export scalability | Exports use `Pageable.unpaged()` and load all matches into memory. | Large exports may cause high memory use or timeouts. | Stream or chunk exports, enforce maximum record/byte limits, and add a scale test. | Resolved |
| P-12 | Resource export indexing | The schema has a composite `(resource_type, resource_id)` index but resource export filters only by `resource_id`. | PostgreSQL may not use the composite index efficiently for resource-only exports. | Add a standalone `resource_id` index or revise the query/filter contract. | Resolved |
| P-13 | Time-range validation | Requests with `from > to` are not explicitly rejected. | Invalid client input can silently return an empty result. | Add cross-field validation and return HTTP 400 with a clear message. | Resolved |
| P-14 | Input validation coverage | Controller tests do not cover every endpoint's malformed body, path, query, and authorization behavior. | API regressions may pass service tests unnoticed. | Add MockMvc tests for all Scenario B/C endpoints and error statuses. | Open |
| P-15 | Concurrency testing | No PostgreSQL integration test demonstrates concurrent append behavior. | Chain safety under real transaction isolation is unproven. | Add a multi-threaded PostgreSQL test after the append-locking design is selected. | Open |
| P-16 | Migration strategy | `hibernate.ddl-auto=update` is used for local startup. | Schema evolution is implicit and may be unsafe or incomplete across environments. | Add Flyway/Liquibase migrations and use `validate` or a controlled mode outside local development. | Open |
| P-17 | Schema invariants | DDL lacks checks for hash format, algorithm, archive/redaction timestamp consistency, and lifecycle states. | Invalid integrity metadata can enter the database. | Add PostgreSQL `CHECK` constraints and integration tests for invalid rows. | In progress |
| P-18 | Security credentials | Local fallback credentials such as `change-me-admin` work when environment variables are absent. | A misconfigured deployment could expose predictable credentials. | Fail fast outside a local profile when credentials are unset; require secret management and TLS in deployment. | Open |
| P-19 | Security identity | Users are in-memory HTTP Basic users. | No lifecycle management, MFA, federation, or scalable identity model exists. | Integrate an external identity provider, token-based authentication, MFA, and role/resource authorization. | Accepted limitation |
| P-20 | Security operations | No TLS, rate limiting, lockout, audit-access monitoring, or anomaly detection is implemented. | Production exposure and abuse may go undetected. | Add TLS, gateway controls, rate limiting, monitoring, alerting, and security-event audit trails. | Accepted limitation |
| P-21 | Full-chain performance | Verification loads and checks the complete chain for every request. | Latency and memory use grow with history size. | Add streaming/range verification, checkpoints, anchored segments, or a maintained integrity summary. | Open |
| P-22 | Hash input framing | Hash input uses a delimiter-separated string. | Delimiter-containing values can create ambiguous concatenations. | Hash a canonical structured object or use explicit field names and length framing; add collision-boundary tests. | Open |
| P-23 | Export completeness | A filtered export is not proof that no records outside the filter exist or that the source chain is globally complete. | Recipients may overstate what the bundle proves. | Include scope, source checkpoint, record count, range boundaries, and an independently verifiable completeness proof. | Accepted limitation |
| P-24 | Page response stability | Spring Data warns that direct `PageImpl` JSON serialization is not a stable API contract. | Client integrations may break after framework changes. | Define a stable page response DTO or configure supported Spring Data page serialization. | Open |
| P-25 | Documentation evidence | Scenario A contains screenshot placeholders and live-defense notes may be incomplete. | Reviewers lack visible evidence of execution and defense readiness. | Capture real Swagger, append, query, verify, and tamper screenshots; complete `docs/11-live-defence-notes.md`. | In progress |
| P-26 | Duplicate documents | Older misspelled Scenario files may remain empty beside canonical documents. | Reviewers may find conflicting or incomplete documentation. | Remove duplicates or add redirect notes pointing to canonical filenames. | Resolved |
| P-27 | AI traceability evidence | Prompt history is raw and does not independently show dates, outputs, rejected alternatives, or command evidence for every step. | The authenticity and reviewability of AI-assisted execution is weaker. | Continue updating `ai-usage/AI_USAGE_LOG.md` with prompt intent, accepted/modified/rejected output, human rationale, artifacts, and validation. | In progress |
| P-28 | Test environment contamination | The local database contains a deliberately inconsistent smoke-test row. | A normal verification run reports a failure before a fresh demo begins. | Recreate a clean local database before final validation, then run the tamper test separately and document the state. | Open |
| P-29 | Retention test coverage | Retention behavior is implemented but lacks a dedicated automated service/controller test. | Soft archival and preservation of hashes are not fully regression-protected. | Add service and API tests asserting archived metadata changes and unchanged integrity fields. | Resolved |
| P-30 | Export/compliance test coverage | Export and compliance implementations lack dedicated automated service/controller coverage. | Filtering, metadata, archive scope, ordering, and empty results can regress. | Add focused unit and MockMvc tests for both API groups. | In progress |

## P-01 Progress Note

**Current status:** In progress.

`AuditEventService` now acquires a transaction-scoped PostgreSQL advisory lock before reading the latest chain hash. This serializes append operations across application instances sharing the database instead of relying on a JVM-local lock.

Validation completed:

- `AuditEventServiceTest` verifies that the advisory-lock query is invoked.
- Full project compilation passes.

Remaining evidence:

- Add a PostgreSQL integration test with concurrent append requests.
- Confirm continuous `previousHash` links under contention.
- Test lock behavior during transaction rollback and database contention.

## Suggested Fix Sequence

### Phase 1: Correctness and integrity

1. Resolve concurrent append serialization (`P-01`, `P-15`).
2. Decide the chain-head/checkpoint boundary (`P-02`, `P-23`).
3. Make compliance behavior deterministic and complete (`P-04`, `P-05`, `P-06`).
4. Make redaction cumulative and path-aware (`P-07`, `P-08`).
5. Strengthen export commitments and bounds (`P-10`, `P-11`, `P-12`).

### Phase 2: Test and data quality

1. Add controller/service/integration tests (`P-14`, `P-29`, `P-30`).
2. Add time-range validation (`P-13`).
3. Add schema constraints and migration support (`P-16`, `P-17`).
4. Remove database contamination before final demonstration (`P-28`).

### Phase 3: Security and operations

1. Remove unsafe credential fallbacks outside local development (`P-18`).
2. Plan external identity, TLS, rate limiting, and monitoring (`P-19`, `P-20`).
3. Improve full-chain verification scalability (`P-21`).
4. Replace delimiter-only hashing (`P-22`).
5. Stabilize page responses (`P-24`).

### Phase 4: Submission evidence

1. Complete live-defense notes and screenshots (`P-25`).
2. Clean duplicate documentation (`P-26`).
3. Maintain complete AI traceability (`P-27`).

## Update Protocol

When a problem is addressed:

1. Change its status to **In progress** before editing.
2. Record the implementation file or migration that changed.
3. Add the test or manual validation used.
4. Change the status to **Resolved** only after validation passes.
5. If the issue is intentionally deferred, keep **Accepted limitation** and record the rationale in the relevant design document.
6. Add the same decision and validation evidence to `ai-usage/AI_USAGE_LOG.md`.

## Current Assessment

The repository is a credible prototype with broad API and documentation coverage. The highest-value next work is to protect linear append semantics, close the compliance/redaction/export correctness gaps, increase integration-test coverage, clean the submission evidence, and make the prototype's security and data-integrity boundaries explicit.

## Latest Validation Note

The current focused correctness suite passes 41 tests after adding redaction, compliance, export, time-range, and retention coverage. Full-suite confirmation is required before marking the newly added retention coverage resolved.
