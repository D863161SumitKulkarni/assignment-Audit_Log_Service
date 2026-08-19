# Requirement Analysis

## Objective

Build a backend-only, tamper-evident audit log service that stores audit events in an append-only chain, supports controlled querying and verification, and provides a clear foundation for retention, redaction, bulk export, and compliance reporting.

The service will be implemented with Java 21, Spring Boot 3, Maven, and PostgreSQL. The assignment will be validated through its APIs; no external consumer or frontend is required.

## Functional Requirements

### Audit event ingestion

- The service must accept and store append-only audit events.
- Every event must include:
	- `eventType`
	- `actorId`
	- `resourceType`
	- `resourceId`
	- `payload`
	- `timestamp`
- The service must assign the event timestamp on the server to prevent backdated writes.
- The public API must not expose update or delete operations.
- Each stored record must include `previousHash` and `currentHash`.
- `currentHash` must be calculated using SHA-256 over the record's agreed canonical content, including the preceding hash commitment.

### Querying

- The query API must support filtering by:
	- `actorId`
	- `resourceType`
	- `resourceId`
	- `eventType`
	- a timestamp range
- Query results must support pagination.
- Query responses must return a redacted payload where applicable, while retaining the original payload's hash commitment for integrity verification.

### Chain verification

- `GET /audit/verify` must verify the audit chain.
- Verification must detect broken or inconsistent hash links.
- The response must report whether verification succeeded and identify the first inconsistency when verification fails.

### Scenario B: retention, redaction, and export

- The service must support retention handling for records that reach the configured retention boundary.
- Retention must use soft archival rather than physically deleting audit records.
- Redaction must be structured and deterministic, rather than an untracked modification of stored event content.
- Bulk export must provide a controlled way to export matching audit records, with the applicable redaction and integrity metadata preserved.

### Scenario C: compliance reporting

- The service must support the compliance use case in which regulators audit access to client account data.
- Reporting must be based on audit events that identify the actor, accessed resource, event type, and time.
- The reporting contract and regulator-specific output format must remain explicit until the assignment provides further compliance details.

### API review and traceability

- The API must be documented with Swagger/OpenAPI for review and validation.
- The repository must include AI usage traceability and honest notes describing AI-assisted execution, including meaningful prompts, generated guidance, human decisions, and validation performed.

## Non-Functional Requirements

- **Integrity:** Hash chaining must make unauthorized insertion, removal, or alteration detectable.
- **Immutability:** Existing audit records must remain immutable through the public API.
- **Consistency:** Server-assigned timestamps and canonical hashing must produce deterministic integrity checks.
- **Security:** Audit data and exports must be exposed only through intentionally defined endpoints and controls.
- **Operability:** Verification failures must identify the first inconsistency so investigation can begin quickly.
- **Performance:** Pagination and indexed filters should support practical audit-history queries without requiring unbounded result sets.
- **Maintainability:** The service should use clear API contracts, documented assumptions, and a technology stack that is straightforward to build and test.
- **Reviewability:** OpenAPI documentation and repository-level AI traceability must make the implementation decisions auditable.

## Assumptions

- PostgreSQL is the system of record for audit events.
- The service is deployed as a single logical chain for the assignment; partitioning or distributed-chain coordination is not required unless later specified.
- The server clock is trusted and synchronized sufficiently for the stated time-range queries.
- `payload` is structured data and can be redacted according to defined fields or rules.
- A soft-archived record remains available for integrity verification and authorized export unless a stricter retention policy is introduced.
- SHA-256 is sufficient for the assignment's tamper-evidence requirement; this is detection of modification, not proof of a malicious actor's identity.
- Authentication, authorization, and regulator identity management are required production concerns but are not fully defined by the assignment.

## Explicit Ambiguity Handling

The statement, "Regulators need to be able to audit access to client account data," does not define the required report shape, regulatory framework, actor roles, data classifications, access categories, retention period, or authorization model.

For this assignment, the requirement is interpreted as a traceability report over audit events involving client account resources. The report should be able to identify who accessed what, which access event occurred, and when it occurred, using `actorId`, `resourceType`, `resourceId`, `eventType`, and the timestamp range.

The implementation will document this interpretation and keep the reporting boundary extensible. A production implementation would confirm the applicable regulation, report format, access taxonomy, privacy constraints, and regulator permissions before treating the compliance contract as complete.

## Out of Scope

- Frontend or user-interface development.
- Public update or delete endpoints for audit events.
- Physical deletion as part of retention processing.
- A specific regulatory certification or legal compliance determination.
- Full identity, tenant, role, and permission management.
- External SIEM, message broker, object storage, or archival integrations.
- Cryptographic signatures, external notarization, or a trust anchor beyond the SHA-256 chain.
- Real-time alerting and anomaly detection.
- A finalized regulator-specific compliance report format.

## Acceptance Criteria

- A valid event can be appended with all required fields, with the timestamp assigned by the server.
- Stored events contain correctly linked `previousHash` and `currentHash` values generated with SHA-256.
- The API exposes append, query, and verification capabilities without update or delete operations.
- Querying supports all specified filters, timestamp ranges, and pagination.
- `GET /audit/verify` reports a successful chain and identifies the first inconsistent record after a deliberate chain inconsistency.
- Retention changes a record to a soft-archived state without removing it from the integrity chain.
- Structured redaction changes the response representation without invalidating the original hash commitment.
- Bulk export returns the selected records with their integrity metadata and applicable redaction.
- Compliance reporting can identify access to client account resources by actor, resource, event type, and time.
- Swagger/OpenAPI documentation describes the available API contracts.
- The repository contains an AI usage traceability log and honest AI-assisted execution notes.

## Engineering Ownership Statement

I own the requirements interpretation, architecture, implementation decisions, and validation of this submission. AI tools may assist with brainstorming, drafting, code suggestions, and review, but all generated material is evaluated, adapted, tested, and integrated by me. The repository's AI usage traceability records the assistance used and the human decisions made throughout development.
