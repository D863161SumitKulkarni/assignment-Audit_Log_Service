# Architecture Overview

## 1. System Purpose

The audit log service is a backend API-only application for recording and reviewing tamper-evident audit events. It stores events in PostgreSQL, links records with a SHA-256 hash chain, exposes append-only write behavior, and supports query, verification, retention, redaction, export, and clarified compliance-reporting workflows.

The service deliberately has no frontend or external consumer. Swagger/OpenAPI is provided for API review and manual validation.

## 2. Component Diagram

```text
+----------------------+       +----------------------+
| API Client           |       | Swagger UI / OpenAPI |
| admin or auditor     |       | public documentation |
+----------+-----------+       +----------+-----------+
           |                              |
           +---------------+--------------+
                           v
                +------------------------+
                | Spring Security        |
                | HTTP Basic, roles,     |
                | stateless requests     |
                +-----------+------------+
                            v
                +------------------------+
                | REST Controllers       |
                | audit, verify,         |
                | retention, redaction,  |
                | export, compliance     |
                +-----------+------------+
                            v
                +------------------------+
                | Application Services  |
                | append/query, hash,    |
                | verify, retention,     |
                | redact, export, report |
                +------+----------+------+
                       |          |
             +---------v--+   +---v----------------+
             | JsonUtil   |   | AuditEventMapper   |
             | canonical  |   | entity to response |
             | JSON       |   | with redaction    |
             +---------+--+   +-------------------+
                       |
             +---------v--------------------------+
             | Spring Data JPA                    |
             | repository and dynamic Specifications|
             +----------------+--------------------+
                              v
                    +-----------------------+
                    | PostgreSQL            |
                    | audit.event           |
                    | durable audit records |
                    +-----------------------+
```

A request enters through Spring Security and a REST controller, moves through an application service, and reaches PostgreSQL through the repository layer. Hashing and canonical JSON are shared utilities used by append and verification. Response mapping is kept separate so redaction does not alter integrity inputs.

## 3. Package Structure

```text
com.auditlog
├── audit_log_service
│   └── AuditLogServiceApplication
├── config
│   └── SecurityConfig
├── controller
│   ├── AuditEventController
│   ├── AuditVerificationController
│   ├── ComplianceReportController
│   ├── ExportController
│   ├── RedactionController
│   └── RetentionController
├── dto
│   ├── AuditEventResponse
│   ├── ComplianceReportResponse
│   ├── CreateAuditEventRequest
│   ├── ExportBundleResponse
│   ├── RedactAuditEventRequest
│   └── VerifyChainResponse
├── entity
│   └── AuditEvent
├── exception
│   ├── ApiErrorResponse
│   ├── GlobalExceptionHandler
│   └── ResourceNotFoundException
├── repository
│   ├── AuditEventRepository
│   └── AuditEventSpecifications
├── service
│   ├── AuditEventMapper
│   ├── AuditEventService
│   ├── ChainVerificationService
│   ├── ComplianceReportService
│   ├── ExportService
│   ├── HashService
│   ├── RedactionService
│   └── RetentionService
└── util
    └── JsonUtil
```

- `config` defines cross-cutting application security.
- `controller` exposes HTTP contracts and does not contain persistence logic.
- `dto` defines request and response boundaries.
- `entity` models the durable audit record.
- `repository` provides persistence and dynamic query predicates.
- `service` owns business workflows and integrity rules.
- `exception` provides consistent API error responses.
- `util` provides deterministic JSON support for hashing.

## 4. Data Model Summary

The `audit.event` table is the durable store. Important fields include:

| Field group | Fields | Purpose |
| --- | --- | --- |
| Identity | `id`, `event_id` | Database ordering and public event identity. |
| Business event | `event_type`, `actor_id`, `resource_type`, `resource_id` | Describes who performed what action on which resource. |
| Payload | `payload_original`, `payload_redacted` | Retains the integrity source and optional response representation. |
| Time | `event_timestamp`, `created_at` | Server-assigned event time and persistence time. |
| Integrity | `previous_hash`, `current_hash`, `hash_algorithm` | Links the event into the SHA-256 chain. |
| Lifecycle | `archived`, `archived_at`, `redacted`, `redacted_at`, `redaction_reason` | Stores controlled retention and redaction metadata. |

The public API does not expose update or delete operations. The current database schema remains a prototype and still needs database-level mutation controls for a stronger append-only guarantee.

## 5. API Groups

| Group | Endpoints | Function |
| --- | --- | --- |
| Audit events | `POST /api/audit/events`, `GET /api/audit/events` | Append and filter audit events with pagination. |
| Verification | `GET /api/audit/verify` | Verify the complete chain and report the first inconsistency. |
| Retention | `POST /api/audit/retention/archive` | Soft-archive events older than a configured number of days. |
| Redaction | `POST /api/audit/events/{eventId}/redact` | Create a controlled redacted response representation. |
| Export | `GET /api/audit/export/actor/{actorId}`, `GET /api/audit/export/resource/{resourceId}` | Return a self-contained filtered export bundle. |
| Compliance | `GET /api/audit/compliance/client-account-access` | Report selected client-account access events under the Scenario C interpretation. |
| Documentation | `/swagger-ui/index.html`, `/v3/api-docs` | Review and exercise the API contract. |

Spring Security permits the documentation resources publicly. Read-only investigation endpoints require `AUDITOR` or `ADMIN`; append, redaction, and retention require `ADMIN`.

## 6. Hash Chain Design

1. The first record uses a genesis `previousHash` consisting of 64 zero characters.
2. Each later record uses the preceding record's `currentHash` as its `previousHash`.
3. The hash input includes, in deterministic order, `eventType`, `actorId`, `resourceType`, `resourceId`, canonical original payload, event timestamp, and previous hash.
4. A clear delimiter separates the fields.
5. SHA-256 produces a lowercase hexadecimal `currentHash`.
6. Verification reads all records in ascending database `id` order.
7. Verification checks the previous-link value before recalculating the current record hash.
8. Verification reports the first `PREVIOUS_HASH_MISMATCH` or `CURRENT_HASH_MISMATCH` and returns HTTP 200 because a broken chain is a valid verification result.
9. JSONB payloads are parsed and canonicalized before verification so PostgreSQL formatting does not change the hash input.
10. Archived records remain part of verification, and redacted records are verified from `payload_original`, never `payload_redacted`.

A remaining production concern is concurrent append serialization. The service should use a database lock, chain-head row, or PostgreSQL advisory lock to prevent two writers from reading the same latest hash.

## 7. Redaction Design

Redaction is represented separately from the immutable original payload:

- The service reads the original structured payload.
- Requested fields are removed from the response representation.
- The redacted JSON is stored in `payload_redacted`.
- `redacted`, `redacted_at`, and `redaction_reason` record the operation metadata.
- `payload_original`, `previous_hash`, `current_hash`, and `event_timestamp` are not changed.
- `AuditEventMapper` returns `payload_redacted` only when the event is marked redacted and that value exists.

This preserves chain verification because the original payload remains the hash source. The prototype currently handles top-level fields; nested path semantics and cumulative redaction should be defined before production use.

## 8. Retention Design

Retention is soft archival, not deletion:

- `RetentionService` finds unarchived events older than the requested cutoff.
- It sets `archived=true` and `archived_at`.
- It does not change payloads, hashes, event timestamp, or chain order.
- Normal audit queries exclude archived records by default.
- Chain verification continues to include archived records because they remain part of the complete chain.
- The retention endpoint is admin-only and explicitly reports archival rather than deletion.

A production design should replace `ddl-auto=update` with versioned migrations and define legal holds and regulation-specific retention policies.

## 9. Export Design

Exports are filtered by actor ID or resource ID and load matching records in ascending `id` order. The response includes:

- Export timestamp and filter metadata.
- Mapped audit response records.
- The first record's `previousHash`.
- The last record's `currentHash`.
- The declared hash algorithm.
- An export hash over canonical metadata and ordered event IDs.

Empty exports remain valid HTTP 200 responses with an empty record list and null chain boundaries. The current export hash proves the selected metadata and boundaries, but a stronger production bundle should commit every record's hash or canonical record content and use streaming/chunking for large exports.

## 10. Compliance Reporting Design

Scenario C is normalized as follows:

- `resourceType` must equal `CLIENT_ACCOUNT`.
- Included event types are `ACCOUNT_VIEWED`, `ACCOUNT_EXPORTED`, `ACCOUNT_UPDATED`, and `PERMISSION_GRANTED`.
- Optional filters include client account ID, actor ID, and an event timestamp range.
- Archived records are excluded by default in the current prototype.
- Results are mapped to the standard audit response shape.
- The report includes generation time, client account ID, returned record count, and access events.

This is an assignment-level traceability report, not a claim of legal compliance. Regulator identity, authorization, report format, retention obligations, and data classification remain production questions.

## 11. Key Design Decisions

- Build a backend API only because the assignment is validated through APIs.
- Use PostgreSQL as the durable store.
- Use server-assigned timestamps to prevent caller-supplied backdating.
- Use SHA-256 for a readable tamper-evidence chain.
- Keep the public audit API append-only.
- Preserve original payloads and hash commitments when returning redacted responses.
- Use soft archival so historical records remain verifiable.
- Use Spring Data specifications for composable query filters.
- Use Swagger/OpenAPI for contract review.
- Use stateless HTTP Basic with role separation for this prototype.
- Read credentials from environment variables rather than committing secrets.

## 12. Trade-offs

| Decision | Benefit | Cost |
| --- | --- | --- |
| SHA-256 chain | Simple, available, deterministic, easy to demonstrate. | Detects changes but does not provide non-repudiation or external proof. |
| Database ID ordering | Simple chain traversal and stable pagination. | Requires concurrency control and careful handling of database writes. |
| JSONB payload storage | Flexible structured event data and PostgreSQL querying potential. | Database formatting must be canonicalized before verification. |
| Soft archival | Preserves verification and historical evidence. | Storage grows and ordinary queries need archive filtering. |
| In-memory users | Small and reviewable assignment implementation. | Not suitable for lifecycle management, MFA, or multi-instance production. |
| HTTP Basic | Easy Swagger and local testing. | Requires TLS and should eventually be replaced with managed identity/token auth. |
| Direct `Page` responses | Fast to implement. | Spring Data warns that serialized page shape is not a stable API contract. |
| `Pageable.unpaged()` export | Simple self-contained bundle. | Can consume significant memory for large result sets. |

## 13. Production Hardening Opportunities

- Serialize concurrent appends with a database lock or protected chain-head record.
- Add database triggers, restricted roles, or write-only procedures to enforce append-only storage against privileged SQL changes.
- Use Flyway or Liquibase migrations instead of `ddl-auto=update`.
- Move credentials to a secrets manager and require TLS for database and HTTP traffic.
- Replace in-memory users with an external identity provider, MFA, token rotation, and account lifecycle management.
- Add tenant and resource-level authorization for exports, redaction, retention, and compliance reports.
- Add rate limiting, lockout, audit-access logging, monitoring, and alerting.
- Use structured hash inputs with field names or length framing instead of delimiter-only concatenation.
- Make redaction nested, cumulative, policy-driven, and tested against sensitive data classifications.
- Strengthen export integrity by committing every selected record's hash or canonical content.
- Stream large exports and apply explicit maximum export sizes.
- Return a stable page DTO and expose total matching count separately from current page size.
- Validate `from <= to` and add comprehensive controller integration tests.
- Define legal holds, regulator permissions, retention periods, and report formats with stakeholders.
