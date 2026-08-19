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

Prompt 11 Controller Creation

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

Create AuditVerificationController. java in package com. auditlog.controller.
Base path: /api/audit
Endpoint:
GET / verify
Response:
Verify ChainResponse
Requirements:
1. Use ChainVerificationservice.
2. Return HTTP 200 for both intact and broken chain results.
3. Do not throw exception when chain is broken because broken chain is a valid verification result.
4. Use @RestController.
5. Use ResponseEntity.
6. Output complete Java file only.

Test SQL Tamper detection

Create database/tamper-test.sql for PostgresQL.
Purpose:
Demonstrate tamper detection for the audit log service.
Requirements:
1. Include comments explaining test flow.
2. Query first few audit event records.
3. Directly update payloadoriginal of one existing record without updating current hash.
4. Query the modified record.
5. Include expected result:
- GET /api/audit/verify should return chainIntact false
- violationType should be CURRENT_HASH_MISMATCH
6. Include a rollback note for local testing only.
7. Output SQL only.|
=======================================================

SCENERIO B Retention

=================================================

Prompt 1 

Retention Service 

Create RetentionService.java in package com.auditlog.service.
Purpose:
Implement retention archival for audit events without breaking hash chain verification.
Dependencies:
AuditEventRepository
Requirements:
1. Method archiveEventsOlderThan(int days) returns long count.
2. Find records with eventTimestamp older than Instant.now(). minus (days) .
3. For each matching record:
- set archived true
- set archivedAt Instant.now()
4. Do not modify payloadoriginal, previousHash, currentHash, or eventTimestamp.
5. Do not physically delete records.
6. Use @Transactional.
7. Explain in code comments that archived records remain part of chain verification.
8. Output complete Java file only.

Retention Controller

Create RetentionController. java in package com. audit-og. contreuter.
Base path:
/api/audit/retention
Endpoint:
POST /archive
Query param.
days optional integer default 90
Response:
Map with archivedCount and message.
Requirements:
1. Use RetentionService.
2. Use ResponseEntity.
3. Do not delete records.
4. Make it clear this is archival only.
5. Output complete Java file only.

====================================================

Scenerio B Redaction

====================================================

Prompt 1

Create RetentionController-java in package com.audit-og. controtter.
Base path:
/api/audit/retention
Endpoint:
POST /archive
Query param:
days optional integer default 90
Response:
Map with archivedCount and message.
Requirements:
1. Use RetentionService.
2. Use ResponseEntity.
3. Do not delete records.
4. Make it clear this is archival only.
5. Output complete Java file only.

Create RedactionController.java in package com. auditlog. contreller•
Base path:
/api/audit/events
Endpoint:
POST /{eventId}/redact
Path variable:
UUID eventId
Request:
RedactAuditEventRequest
Response:
AuditEventResponse
Requirements:
1. Use Redactionservice.
2. Use @Valid on request body.
3. Use ResponseEntaty.
4. Do not expose general update API.
5. This endpoint is only for controlled redaction metadata and redacted response payload.
6. Output complete Java file only.

===================================================

Scenerio B Bulk Export

==================================================

Prompt 1

Create ExportService. java in package com.auditlog.service.
Purpose:
Export audit events for a given actorId or resourceId as a self-contained verifiable bundle.
Dependencies:
AuditEventRepository
AuditEventMapper
HashService
Isonutil
Methods:
1. ExportBundleResponse exportByActorId(String actorId)
2. ExportBundleResponse exportyResourceId (String resourceld)
Requirements:
1. Load matching records ordered by id ascending.
2. Include mapped AuditEventResponse records.
3. Include firstRecordPreviousHash from first record.
4. Include lastRecordCurrentHash from last record.
5. Include hashAlgorithm SHA-256.
6. Compute exportHash using SHA-256 over:
- filterType
- filterValue
- exported record eventIds in order
- firstRecordPreviousHash
- LastRecordCurrentHash
7. Use Instant. now() for exportedAt.
8. If no records found, return empty records, null chain boundaries, and exportash over empty bundle metadata.
9. Output complete Java file only.

Create ExportController.java in package com.auditlog.controller.
Base path:
/api/audit/export
Endpoints:
1. GET /actor/{actorId)
2. GET /resource/(resourcela}
Response:
ExportBundleResponse
Requirements:
1. Use ExportService.
2. Use ResponseEntity.
3. Return HTTP 200 even if bundle is empty.
4. Output complete Java file only.

======================================================

Scenerio C Compliance Reporting

===================================================

Prompt 1 

Create docs/08-scenario-c-compliance-reporting.md.

Original
ambiguous requirement:
"Regulators need to be able to audit access to client account
data."
Create a professional ambiguity analysis.
Include:
1. Why the requirement is ambiguous.
2. Clarifying questions:
- What counts as client account data?
- Which event types represent access?
- Who are regulators?
- What time range is required?
- Is export required?
- Should sensitive payload data be redacted?
-What format
is required?
- What retention period applies?
3. Assumptions made for this prototype.
4. Normalized requirement statement.
5. Technical design.
6. Implemented scope.
7. Out of scope.
8. Validation approach.
9. Risks and trade-offs.
I
My chosen implementation:
Create a compliance report endpoint that returns audit events where resourceType equals CLIENT_ACCOUNT and eventType is one of ACCOUNT_VIEWED, ACCOUNT_EXPORTED, ACCOUNT_UPDATED, PERMISSION_GRANTED.
Use markdown.
Do not include Java code.

Prompt 2 Services and Controller

Create ComplianceReportService. java in package com auditlog service.
Purpose:
Implement Scenario C compliance reporting for client account data access.
Dependencies:
AuditEventRepository
AuditEventMapper
Method:
public ComplianceReportResponse getClientAccountAccessReport(
String clientAccountIa,
String actorId,
Instant from,
Instant to
Pageable pageable
)

Rules:
1. resourceType must be CLIENT_ACCOUNT.
2. If ctientAccountId is provided, filter resourceId equals stientAccountId.
3. If actorId is provided, filter actorId equals actorid.
4. Only include eventType values:
- ACCOUNT_VIEWED
- ACCOUNT_EXPORTED
- ACCOUNT.
_UPDATED
- PERMISSION_GRANTED
5. Support from and to filters on eventTimestamp.
6. Exclude archived records by default.
7. Map results to AuditEventResponse.
8. totalRecords should represent returned page content size for prototype simplicity.
9. Output complete Java file only.

Create ComplianceReportController.java in package com.auditlog.contcoller.
Base path:
/api/audit/compliance
Endpoint:
GET /client-account-access
Query params:
- clientAccountid optional
- actorid optional
- from optional Instant ISO-8601
- to optional Instant ISO-8681
- page default e
- size default 20
Response:
ComplianceReportResponse
Requirements:
1. Use ComplianceReportService.
2. Use ResponseEntity.
3. Add comments that this endpoint implements the clarified Scenario C prototype scope.
4. Output complete Java file only.

=========================================================

Global Exception Handler

====================================================

Prompt 

Create exception handling classes for package com.auditlog.exception.
Files:
1. ResourceNotFoundException
2. ApiErrorResponse
3. GlobalExceptionHandler
Requirements:
1. ResourceNotFoundException extends RuntimeException.
2. ApiErrorResponse fields:
- Instant timestamp
- int status
- String error
- String message
- String path
3. GlobalExceptionHandler handles:
- ResourceNotFoundException with 404
- MethodArgumentNotValidException with 400
- IllegalArgumentException with 400
- generic Exception with 500
4. Use @RestControllerAdvice.
5. Use Spring Boot 3 and Java 21.
6. Output complete code for all three files, clearly separated by file name.

============================================================

TESTS Entities (As per scenerios)

==========================================================

prompt 1 Hash service test

Create HashServiceTest. java for package com.auditlog service.
Test class:
HashserviceTest
Requirements:
1. Use JUnit 5.
2. Test sha256 returns 64 character lowercase hex.
3. Test same input produces same hash.
4. Test different input produces different hash.
5. Test calculateAuditEventHash includes previousHash by showing different previousHash changes output.
6. Test null input throws IllegalArgumentException.
7. Output complete test file only.

prompt 2 Audit Event Service test

Create AuditEventServiceTest.java for package com. auditlog.service.
Purpose:
Unit test append-only audit event creation.
Use:
JUnit 5
Mockito
Mock:
AuditEventRepository
HashService
Isonutil
AuditEventMapper
Test cases:
1. First event uses genesis previousHash.
2. Second event uses latest record currentash as previousHash.
3. createEvent saves AuditEvent with generated currentHash.
4. createEvent assigns server timestamp.
5. createEvent does not call any update or delete operation.
Keep test focused and compile-ready.
Output complete Java test file only.

prompt 3 chain verification service test

Create ChainVerificationServiceTest. java for package com.auditlog. service.
Use:
JUnit 5
Mockito
Mock:
AuditEventRepository
HashService
Test cases:
1. Empty chain returns chainIntact true.
2. Valid chain with two records returns chainIntact true.
3. previousHash mismatch returns chainIntact false and violationType PREVIOUS_HASH_MISMATCH.
4. currentash mismatch returns chainIntact false and KielationType CURRENT_HASH_MISMATCH.
5. Archived record is still included in verification.
6. Redacted record verifies using payloadoriginal.
Output complete test file only.

Prompt 4 Redaction Service Test

Create RedactionServiceTest. java for package com. auditlog.secvice.
Use:
JUnit 5
Mockito
Test cases:
1. Redacts existing top-level payload field.
2. Ignores missing field without failing.
3. Sets redacted true.
4. Sets redactedAt.
5. Stores redactionReason.
6. Does not modify paytoadoriginat.
7. Does not modify currentHash or previoushash.
8. Throws ResourceNotFoundException when eventId is missing.
Output complete test file only.
=========================================================

Architecture overview

=========================================================

prompt 

Create docs/03-architecture-overview.md for the audit log service.
Include:
1. System purpose.
2. Component diagram in text form.
3. Package structure.
4. Data model summary.
5. API groups.
6. Hash chain design.
7. Redaction design.
8. Retention design.
9. Export design.
10. Compliance reporting design.
11. Key design decisions.
12. Trade-offs.
13. Production hardening opportunities.
Mention:
- Backend API only.
- PostgreSQL as durable store.
- SHA-256 hash chain.
- Append-only API.
I
- Server-assigned timestamps.
- Archived records remain part of verification.
- Redacted responses preserve chain verification by not changing immutable original payload.
Use clear markdown.

=========================================================

API Design DOC

=========================================================

prompt 

Create docs/04-api-design.md.
Document all APIs:
1. POST /api/audit/events
2. GET /api/audit/events
3. GET /api/audit/verify
4. POST/api/audit/retention/archive
5. POST/api/audit/events/(eventId}/redact
6. GET /api/audit/export/actor/{actorId}
7. GET /api/audit/export/resource/{resourceld}
8. GET /api/audit/compliance/client-account-access
For each API include:
- Purpose
- Request parameters
- Sample request
- Sample response
- Validation rules
- Notes
Mention explicitly:
No update or delete API is exposed for audit events.
Use markdown.

========================================================

Hash Chain Design Doc

=========================================================

prompt 

Create docs/05-hash-chain-design.md.
Explain the tamper-evident hash chain design.
Include:
1. Goal.
2. Fields included in currentHash.
3. RreviousHash behavior.
4. Genesis hash value.
5. SHA-256 rationale.
6. Server-assigned timestamp rationale.
7. Verification algorithm step by step.
8. Violation types:
- PREVIOUS_HASH_MISMATCH
- CURRENT_HASH_MISMATCH
9. Why direct database tampering is detected.
10. Limitations:
- Database administrator can modify all hashes if fully malicious.
- Stronger production system would use external anchoring, signatures, WORM storage, or immutable ledge
11. Why archived and redacted records do not break verification.
Use clear markdown.

=======================================================

Scenerio A Validation 

=======================================================

prompt 

Create docs/06-scenario-a-validation.md.
Document how to validate Scenario A.
Include:
1. Start application.
2. Create first audit event using curl.
3. Create second audit event using curl.
4. Query events by actorId.
5. Query events by resourceType and resourcela.
6. Query events by eventType.
7. Query events by time range.
8. Verify chain using GET /api/audit/verify.
9. Tamper directly in PostgreSQL using database/tamper-test.sq1.
10. Run verify again and expect chainIntact false.
11. Expected violation type.
12. Screenshots placeholder section.
13. Troubleshooting notes.
Use markdown and curl examples.

=======================================================

Scenerio B Doc

=======================================================

prompt 

Create docs/07-scenario-b-retention-redaction-export.md.
Document Scenario B implementation.
Sections:
1. Overview.
2. Retention policy:
- soft archival
- configurable window
- no physical deletion
- archived records remain in chain verification
3. Structured redaction:
- Ray-oadoriginal remains immutable
- paxloadRedacted is returned to API clients
- hash chain continues using payloadoriginal
- trade-off: sensitive data is hidden from API but still stored internally
- production alternative: encryption with key destruction or cryptographic commitments
4. Bulk export:
- export by actorId
- export by resourceld
- includes first previous hash, last current hash, hash algorithm, export hash
- independent verification concept
5. Validation examples using curl.
6. Limitations.
7. Future hardening.
Use clear markdown.

======================================================

Testing Strategy

======================================================
prompt

Create docs/09-testing-strategy.md.
Document testing strategy for the audit log service.
Include:
1. Unit tests.
2. Service tests.
3. Controller/API tests.
4. Manual validation using curl.
5. Manual PostgreSQL tamper test.
6. What is tested:
- hash determinism
- append-only creation
- previousHash linkage
- verification success
- verification failure after tampering
- query filtering
- pagination
- retention archive behavior
- redaction behavior
- export bunale metadata
- compliance reporting
7. What is not tested and why.
8. Quality gates before submission:
- mvn clean test
- swagger check
- verify endpoint check
- tamper SQL check
9. Known limitations.
Use markdown.

================================================================

Risk and Tradeoffs

================================================================

Create docs/10-risks-tradeoffs-limitations.md.
For the audit log service, document:
1. Tamper-evidence versus tamper-prevention.
2. Risk of privileged database administrator modifying all records and hashes.
3. Need for external anchoring in production.
4. SHA-256 choice and limitations.
5. Server-assigned timestamp trade-off.
6. Redaction trade-off:
- API redaction works
- original data remains stored for verification
- stronger production option would use encryption and key destruction or commitments
7. Retention trade-off:
- archive not physical deletion
- preserves verification
8. Export limitations:
- verifies exported bundle integrity
- does not prove global completeness unless additional proof is included
9. Compliance reporting scope limitations.
10. Operational risks:
- concurrency during writes
- database transaction isolation
- performance for full-chain verification
11. Future improvements.
Use professional markdown.

=========================================================

README

=========================================================

prompt

Create root README. md for the audit-log-service repository.
Include:
H1 V

1. Project title.

2. Assignment summary.
3. Tech stack:

- Java 21

- Spring Boot 3

- PostgreSQL

- Maven

- Swagger/OpenAPI

4. What the system does.

5. Repository structure.

6. How to run locally:

- create PostgreSQL database auditlogdb
- update application properties password

- cd backend
-mvn spring-boot: run

7. Swagger URL:

http://localhost:8080/swagger-ui/index.html

8. API summary.

9. Scenario A validation.

10. Scenario B validation.

11. Scenario C validation.

12. How to run tests:

mvn clean test

13. Tamper test instructions.
14. AI usage and traceability.

15. Limitations.

16. Live defense notes.

Use professional markdown.

=================================================================