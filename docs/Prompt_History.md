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

Prompt 3 Updation of pom.xml for required dependencies

Update this Spring Boot Maven pom. xm1 for a Java 21 audit log service.
Requirements:
1. Spring Boot 3.x.
2. Java 21.
3. Dependencies needed:
- spring-boot-starter-web
- spring-boot-starter-data-jpa
- spring-boot-starter-validation
- postgresal runtime driver
- 1ombok optional
- spring-boot-starter-test
- springdoc-openapi-starter-webmvc-ui
4. Keep Maven build clean and simple.
5. Do not add security, Kafka, Docker, or cloud dependencies.
6. Output only the complete updated pom. xml.

Application.properties

Create application properties for a Spring Boot 3 audit log service using PostgresQ
Requirements:
1. Application name: audit-log-service
2. Server port: 8080
3. PostgreSQL URL: jdbc:postgresql://localhost:5432/auditlogdb
4. Username: postgres
5. Password placeholder: manager
5. Hibernate ddl-auto: update for local prototype
7. Show SQL: true
3. Format SQL: true
9. OpenAPI Swagger UI enabled
10. Include application properties for:
- retention window days default 90
- hash algorithm SHA-256
- genesis hash value with 64 zero characters
Jse comments explaining which values should be changed locally. utput only the complete application properties.

=================================================
Prompt 4 Database Scema generation

Create database/schema.sat for a PostgresQL audit log service.

Table name: audit.event

Columns:
1. id BIGSERIAL primary key
2. event id VUID unique not null
3. event type VARCHAR(100) not null
4. actorid VARCHAR (150) not nuil
5. resource type VARCHAR (100) not null
6. resource id VARCHAR (150) not null
7. payload original JSONB not null
8. pay-oad redacted JSONB null
9. event timestamp TIMESTAMPTZ not null
10. created at TIMESTAMPTZ not null default now
11. previous. hash VARCHAR (64) not null
12. currentash VARCHAR (64) not null
13. hash_algorithm VARCHAR (50) not null default 'SHA-256'
14. archived BOOLEAN not null default false
15. archived at TIMESTAMPTZ null
16. redacted BOOLEAN not null default false
17. redacted at TIMESTAMPTZ null
18. redaction reason TEXT null
Indexes:
1. actorid
2. resource type, resource id
3. event type
4. event timestamp
5. current hash
6. previous hash
7. archived

Add comments explaining append-only intent and tamper-evident hash chain.

Output only SQL.

============================================================

