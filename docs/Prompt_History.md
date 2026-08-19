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
Prompt 5 Backend Package Structure

backend/src/main/java/com/auditlog/config
backend/src/main/java/com/auditlog/controller
backend/src/main/java/com/auditlog/dto
backend/src/main/java/com/auditlog/entity
backend/src/main/java/com/auditlog/exception backend/src/main/java/com/auditlog/repository backend/src/main/java/com/auditlog/service backend/src/main/java/com/auditlog/util

==================================================

Prompt 6 Audit Entity Creation

Tech stack:

Java 21, Spring Boot 3, Jakarta Persistence, Lombok, PostgreSQL.

Entity:
audit event

Fields:

1. Long id, generated identity primary key

2. UUID eventId, unique, not null

3. String eventType, not null, max length 100

4. String actorId, not null, max length 150

5. String resourceType, not null, max length 100

6. String resourceld, not null, max length 150

7. String payloadoriginal, not null, column type isonb

8. String payloadRedacted, nullable, column type isonb

9. Instant eventTimestamp, not null

10. Instant createdAt, not null

11. String previousHash, not null, length 64

12. String currentHash, not null, length 64

13. String hashAlgorithm, not null, default SHA-256
14. boolean archived

15. Instant archivedAt

16. boolean redacted

17. Instant redactedAt

18. String redactionReason, column type text

Requirements:

1. Use @Entity and @Table.
2. Use @Prepersist to set eventId if null and createdAt if null.
3. Use Lombok Getter, Setter, NoArgsConstructor, AllArgsConstructor, Builder.
4. Use Jakarta imports, not javax.
5. Do not add business logic here except lifecycle defaults.
6. Keep it production-readable.

Output complete Java file only.

======================================================

Prompt 7 Dto Creation 

Create DTO classes in package com.auditlog.dto for an audit log service.

Use Java 21, Spring Boot 3, Lombok, Jakarta Validation.

Create these DTOS:

1. CreateAuditEventRequest
Fields:
- eventType String, required
- actorld String, required
- resourceType String, required
- resourceld String, required
- payload Map‹String, Object>, required
Note: timestamp is not accepted from caller because server assigns timestamp.

2. AuditEventResponse
Fields:
-UUID eventId
- String eventType
- String actorId
- String resourceType
- String resourceld
- Map<String, object> payload
- Instant eventTimestamp
- Instant createdAt
- String previousHash
- String currentHash
- String hashAlgorithm
- boolean archived
- boolean redacted

3. VerifyChainResponse
Fields:
- boolean chainIntact
- long checkedRecords
- UUID firstBrokenEventId
- Long firstBrokenDatabaseId
- String violationType
- String expectedValue
- String actualValue
- String message

4. RedactAuditEventRequest
Fields:
- List‹String› fieldsToRedact, required
- String reason, required

5. ExportBundleResponse
Fields:
- Instant exportedAt
- String filterType
- String filterValue
- List<AuditEventResponse> records
- String firstRecordPreviousHash
- String lastRecordCurrentHash
- String hashAlgorithm
- String exportHash

6. ComplianceReportResponse
Fields:
- Instant generatedAt
- String clientAccountId
- long totalRecords
- List<AuditEventResponse> accessEvents

Requirements:
- Use Lombok Data, Builder, NoArgsConstructor, AllArgsConstructor.
- Use validation annotations on request DTOs.
- Use java. time. Instant.
- Use iava.util.UUID.
- Use java.util.Map and List.
- Output complete code for all files, clearly separated by file name.

===========================================

Prompt 8 Repositiory Creation

Create AuditEventRepository.java in package com.audit_og. repository.

Entity:
AuditEvent

Requirements:
1. Extend IpaRepository<AuditEvent, Long>.
2. Add Optional<AuditEvent> findTopßyOrderByIdDess() •
3. Add Optional< AuditEvent> findBYEventId(UUID eventId).
4. Add List<AuditEvent> findAllByOrderByIdAsc() .
5. Add Page<AuditEvent> query support for dynamic filters using paspecificationExecutor.
6. Add methods useful for export:
- Page<AuditEvent> findByActorIdOrderByIdAsc(String actorId, Pageable pageable)
- Page<AuditEvent> findByResourceIdOrderByIdAsc(String resourceId, Pageable pageable)
7. Use Java 21 and Spring Data JPA.
8. Output complete Java file only.

Audit event Filters

Create AuditEventSpecifications. java in package com. auditlog.repositery.

Purpose:
Build dynamic JPA Specifications for AuditEvent query filters.

Entity fields:
actorId, resourcelype, resourceld, eventType, eventTimestamp, archived.

Required method:

public static Specification<AuditEvent> withFiaters
String actorId,
String resourceType,
String resourceId, String eventType,
Instant from,
Instant to,
Boolean includeArchived
B IS O
)
Rules:
1. If actorId is non-null, filter exact match.
2. If resourceTyre is non-null, filter exact match.
3. If resourceld is non-null, filter exact match.
4. If eventType is non-null, filter exact match.
5. If from is non-null, eventTimestamp >= from.
6. If to is non-null, eventlimestamp ‹= to.
7. If includeArchived is null or false, archived must be false.
8. Sort will be handled by service/controller, not here.
9. Use Spring Data JPA Specification.
10. Output complete Java file only.

======================================================
Prompt 9 Json Utility

Create JsonUtil. java in package com.auditlog.util.

Purpose:
Provide deterministic JSON serialization for audit hashing.

Requirements:
1. Use Jackson ObjectMapper.
2. Convert Map<String, Object> to canonical JSON string.
3. Ensure stable ordering of object keys.
Exclude null values only if explicitly configured in code comments.
5. Provide method:
public String toCanonicalison(Object value)
6. Provide method:
public Map‹String, Object> fromJsonToMap (String ison)
7. Throw IllegalArgumentException with clear message if serialization fails.
8. Use Java 21.
9. Keep class as @Component.
10. Output complete Java file only.

========================================================

Prompt 10 Core Services and APIs

Create HashService. java in package com.auditlog.service.

Purpose:
Generate SHA-256 hashes for a tamper-evident audit log chain.

Tech stack:
Java 21, Spring Boot 3.

Requirements:
1. Use SHA-256.
2. Output lowercase hex string.
3. Provide method:
public String sha256 (String input)
4. Provide method:
public String calculateAuditEventHash(
String eventType,
String actorId,
String resourceType,
String resourceld,
String payloadCanonicalison,
Instant eventTimestamp,
String previousHash
5. The hash input must include all fields in a deterministic order:
eventType actorId resourceType resourceld payloadCanonicallson eventTimestamp.toString()
previousHash
6. Use a clear delimiter between fields.
7. Validate arguments are not null.
8. Throw IllegalArgumentException for invalid input.
9. Class should be @Service.
10. Output complete Java file only.

Create AuditEventMapper. java in package com auditlogeservice.

Purpose:
Map AuditEvent entity to AuditEventResponse DTO.

Requirements:
1. Use Isonutit to convert payload JSON string to Map‹String, Object>.
2. If AuditEvent is redacted and payloadRedacted is not null, response payload must use paxtoadRedacted.
3. Otherwise response payload must use Rayloadoriginal.
4. Map all response fields.
5. Class should be @Component.
6. Use Java 21.
7. Output complete Java file only.

Create AuditEventService.java in package com.auditlog.service.

Purpose:
Implement core business logic for append-only audit event creation and query.

Tech stack:
Java 21, Spring Boot 3, Spring Data JPA, PostgreSQL.

Dependencies:
AuditEventRepository
Hashservice
Isonutil
AuditEventMapper

Requirements:
I
1. Method createEvent (CreateAuditEventRequest request) returns AuditEventResponse.
2. Server assigns eventTimestamp using Instant.now() .
3. Convert request. payload to canonical JSON.
4. Find latest AuditEvent ordered by id desc.
5. If no latest event exists, use genesis hash of 64 zero characters.
6. previousHash must be latest.currentHash or genesis hash.
7. Calculate currentHash using HashService.
8. Save AuditEvent.
9. Do not implement update or delete.
10. Method queryEvents(...) returns Page<AuditEventResponse›.
11. Query filters:
- actorId
- resourceType
- resourceld
- eventType
- from
- to
- includeArchived
- Pageable
12. Use AuditEventSpecifications.
13. Sort by id ascending by default unless Pageable already has sort.
14. Keep code readable and defensive.
15. Annotate write method with @Transactional.
16. Output complete Java file only.

Create AuditEventController.java in package com.auditlog.controller.
Purpose:
Expose REST APIs for audit event write and query.
Base path:
/api/audit
Endpoints:
1. POST /events
Request body: GreateAuditEventRequest
Response: AuditEventResponse
Status: 201 Created
2. GET /events
Query params:
- actorId optional
- resourcelype optional
- resourceId optional
- eventType optional
- from optional Instant ISO-8601
- to optional Instant ISO-8601
- includeArchived optional Boolean default false
- page default 0
- size default 20
Response: Page<AuditEventResponse>
Requirements:
1. Use @RestController.
2. Use @RequestMapping("/api/audit").
3. Use @Valid.
4. Do not expose update or delete APIs.
5. Add OpenAlI annotations if available.
6. Use ResponseEntity.
7. Output complete Java file only.

Create ChainVerificationService.java in package com.auditlog.service.
Purpose:
Verify the tamper-evident audit log hash chain.
Dependencies:
AuditEventRepository
HashService
Requirements:
1. Verify all AuditEvent records ordered by id ascending.
2. Start expectedPreviousHash with genesis hash of 64 zero characters.
3. For each record:
a. Check record.previousHash equals expectedPreviousHash.
b. Recalculate current hash from stored immutable fields:
- eventType
- actorId
- resourceType
- resourceld
- payloadoriginal
- eventTimestamp
- previousHash
c. Check recalculated hash equals record. currentHash.
d. Set expectedPreviousHash to record. currentHash.
4. Return VerifyChainResponse.
5. If previousHash mismatch, violationType should be PREVIOUS_HASH_MISMATCH.
6. If currentHash mismatch, violationType should be CURRENT_HASH_MISMATCH.
7. If no issues, return chainIntact true and checkedRecords count.
8. Archived records must still be included in verification because they remain part of the chain.
9. Redacted records must verify using payloadoriginal, not payloadRedacted.
10. Class should be @Service.
11. Output complete Java file only.

=====================================================

