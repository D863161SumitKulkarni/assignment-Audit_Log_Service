Prompt 1 : Requirement Gathering
======================================
Create docs/01-requirement-analysis.md for an interview assignment.

Assignment:
Build a tamper-evident audit log service.

Important requirements:
1. store append-only audit events.
2. Event fields must include eventType, actorId, resourceType, resourceld, payload, timestamp.
3. The API must not expose update or delete operations.
4. Query API must support filters by astorid, resourcelype and resourceld, eventType, time range, and pagination.
5. Each record must include previousHash and curcentHash.
6. GET /audit/verify must verify the chain and report the first inconsistency.
7. Scenario B adds retention, structured redaction, and bulk export.
8. Scenario C is ambiguous compliance reporting: "Regulators need to be able to audit access to client account data."
9. The repository must include Al usage traceability and honest AI-assisted execution notes.

My engineering decisions:

1. Build backend API only because the assignment is validated through APIs and no external consumer is required.
2. Use Java 21, Spring Boot 3, Maven, PostgreSQL.
3. Use server-assigned timestamp to prevent backdated writes.
4. Use SHA-256 for hash chain.
5. Use append-only public API.
6. Use soft archival for retention.
7. Use redacted response payload while preserving original hash commitment.
8. Use Swagger/OpenAPI for API review.

Output sections:
- objective
- Functional requirements
- Non-functional requirements
- Assumptions
- Explicit ambiguity handling
- Out of scope
- Acceptance criteria
- Engineering ownership statement
Use clear markdown.

Do not include code.

=======================================
Prompt 2 : Task Decomposition

Create docs/02-task-decomposition.md for the audit log service assignment.

Context:

This is an AI-assisted engineering assignment. The reviewers expect task decomposition, implementation sequencing, validation steps, Al usage traceability, and human revies
Create a detailed task breakdown with dependencies.

Include these phases:

1. Repository setup and documentation scaffold.
2. Requirement analysis and assumptions.
3. Architecture and API design.
4. Spring Boot backend generation.
5. Database schema.
6. Scenario A implementation:
- append-only write API
- query API
- hash chain
- verify endpoint
- tamper detection validation
7. Scenario B implementation:
- retention archival
- structured redaction
- verifiable bulk export
8. Scenario C implementation:
- ambiguity clarification
- compliance report endpoint
- scope boundaries
9. Testing:
- unit tests
- service tests
- controller integration tests
- tamper SQL validation
10. Documentation and final engineering summary.
11. Live defense preparation.
For each task include:
- Goal
- Inputs
- Output artifacts
- Acceptance criteria
- Suggested commit message
- AI usage note

Use markdown tables where helpful.

======================================================

