# Audit Log Service

## Assignment Summary

This repository contains an AI-assisted engineering implementation of a tamper-evident audit log service. The service is designed for API-based validation and provides append-only event creation, filtered queries, SHA-256 chain verification, retention archival, structured redaction, bulk export, and a clarified compliance-reporting workflow.

The implementation is backend API only. No frontend or external consumer is required for the assignment.

## Tech Stack

- Java 21
- Spring Boot 3.5.16
- Spring Data JPA and Jakarta Persistence
- PostgreSQL
- Maven
- Lombok
- Spring Security
- Swagger/OpenAPI
- JUnit 5 and Mockito

## What the System Does

- Stores audit events in PostgreSQL as an append-only public API.
- Assigns event timestamps on the server.
- Links records with a SHA-256 hash chain using a 64-zero genesis hash.
- Supports filters by actor, resource, event type, time range, and pagination.
- Verifies the chain and reports the first inconsistency.
- Archives old records without physical deletion.
- Returns structured redacted payloads while preserving the original hash commitment.
- Exports records by actor ID or resource ID with chain metadata and an export hash.
- Provides a Scenario C report for selected client-account access events.
- Protects APIs with stateless HTTP Basic authentication and `ADMIN`/`AUDITOR` roles.

No update or delete API is exposed for audit events.

## Repository Structure

```text
.
├── ai-usage/
│   └── AI_USAGE_LOG.md
├── backend/
│   └── audit-log-service/
│       ├── pom.xml
│       ├── src/main/java/com/auditlog/
│       │   ├── config/
│       │   ├── controller/
│       │   ├── dto/
│       │   ├── entity/
│       │   ├── exception/
│       │   ├── repository/
│       │   ├── service/
│       │   └── util/
│       └── src/test/java/
├── database/
│   ├── schema.sql
│   └── tamper-test.sql
├── docs/
│   ├── 01-requirement-analysis.md
│   ├── 02-task-decomposition.md
│   ├── 03-architecture-overview.md
│   ├── 04-api-design.md
│   ├── 05-hash-chain-design.md
│   ├── 06-scenario-a-validation.md
│   ├── 07-scenario-b-retention-redaction-export.md
│   ├── 08-scenario-c-compliance-reporting.md
│   ├── 09-testing-strategy.md
│   ├── 10-risks-tradeoffs-limitations.md
│   ├── 11-live-defence-notes.md
│   └── 12-security-design.md
└── ATTESTATION.md
```

## Run Locally

### 1. Create the PostgreSQL database

Create a database named `auditlogdb`:

```sql
CREATE DATABASE auditlogdb;
```

The application expects PostgreSQL on `localhost:5432` with the `postgres` username by default.

### 2. Configure local environment values

The application reads secrets from environment variables. In PowerShell:

```powershell
$env:DB_PASSWORD = "<local-postgres-password>"
$env:AUDIT_ADMIN_PASSWORD = "<local-admin-password>"
$env:AUDIT_AUDITOR_PASSWORD = "<local-auditor-password>"
```

The default local usernames are `admin` and `auditor`. The application properties file contains development placeholders only; do not use them in a shared or production environment.

### 3. Start the backend

The Maven project is under `backend/audit-log-service`:

```powershell
cd backend/audit-log-service
.\mvnw.cmd spring-boot:run
```

The application starts on port `8080`.

For a system with Maven installed, the equivalent command is:

```powershell
mvn spring-boot:run
```

For the first local prototype run, Hibernate can create the `audit` schema namespace and update tables. Use versioned migrations before production deployment.

## Swagger/OpenAPI

Swagger UI:

http://localhost:8080/swagger-ui/index.html

OpenAPI JSON:

http://localhost:8080/v3/api-docs

Swagger and OpenAPI documentation are public for API review. Business endpoints still require authentication.

## API Summary

| Method | Endpoint | Purpose | Role |
| --- | --- | --- | --- |
| `POST` | `/api/audit/events` | Append an audit event. | `ADMIN` |
| `GET` | `/api/audit/events` | Query events with filters and pagination. | `AUDITOR`, `ADMIN` |
| `GET` | `/api/audit/verify` | Verify the complete hash chain. | `AUDITOR`, `ADMIN` |
| `POST` | `/api/audit/retention/archive` | Soft-archive old events. | `ADMIN` |
| `POST` | `/api/audit/events/{eventId}/redact` | Create a controlled redacted response payload. | `ADMIN` |
| `GET` | `/api/audit/export/actor/{actorId}` | Export records for an actor. | `AUDITOR`, `ADMIN` |
| `GET` | `/api/audit/export/resource/{resourceId}` | Export records for a resource. | `AUDITOR`, `ADMIN` |
| `GET` | `/api/audit/compliance/client-account-access` | Scenario C client-account access report. | `AUDITOR`, `ADMIN` |

See [docs/04-api-design.md](docs/04-api-design.md) for request parameters, examples, validation, response contracts, and error statuses.

## Scenario A Validation

Scenario A validates append-only writes, query filters, pagination, hash linkage, verification, and direct tamper detection.

Follow [docs/06-scenario-a-validation.md](docs/06-scenario-a-validation.md) to:

1. Create two linked events with `curl.exe`.
2. Query by actor, resource, event type, and time range.
3. Verify an intact chain.
4. Run the PostgreSQL tamper script.
5. Verify the expected `CURRENT_HASH_MISMATCH` result.

## Scenario B Validation

Scenario B covers:

- Soft retention archival with no physical deletion.
- Structured response redaction while preserving `payload_original` and hash values.
- Exports by actor ID and resource ID with chain boundaries and export hash metadata.

See [docs/07-scenario-b-retention-redaction-export.md](docs/07-scenario-b-retention-redaction-export.md) for curl examples, trade-offs, limitations, and future hardening.

## Scenario C Validation

Scenario C interprets the ambiguous requirement about regulators auditing client account access as:

- `resourceType=CLIENT_ACCOUNT`
- `eventType` in `ACCOUNT_VIEWED`, `ACCOUNT_EXPORTED`, `ACCOUNT_UPDATED`, and `PERMISSION_GRANTED`
- Optional client account, actor, and time-range filters

See [docs/08-scenario-c-compliance-reporting.md](docs/08-scenario-c-compliance-reporting.md) for the ambiguity analysis, normalized requirement, implementation scope, and validation approach.

## Run Tests

From `backend/audit-log-service`, set `DB_PASSWORD` and run:

```powershell
$env:DB_PASSWORD = "<local-postgres-password>"
.\mvnw.cmd clean test
```

The current verified baseline is 32 tests with zero failures, errors, or skips. The test inventory includes hashing, append behavior, chain verification, compliance reporting, redaction, application wiring, security, malformed input, and pagination bounds.

The full testing strategy is documented in [docs/09-testing-strategy.md](docs/09-testing-strategy.md).

## Tamper Test

The direct PostgreSQL tamper procedure is [database/tamper-test.sql](database/tamper-test.sql). It changes `payload_original` without updating `current_hash`, then the verify endpoint should return:

```json
{
	"chainIntact": false,
	"violationType": "CURRENT_HASH_MISMATCH"
}
```

Run it only against a local test database:

```powershell
$env:PGPASSWORD = "<local-postgres-password>"
psql -h localhost -U postgres -d auditlogdb -f database/tamper-test.sql
Remove-Item Env:PGPASSWORD
```

The script commits the deliberate tampering so the running application can detect it. Restore a clean local database afterward rather than repairing hashes silently.

## AI Usage and Traceability

AI was used for requirements analysis, documentation drafting, code generation, test generation, debugging support, and review assistance. Human review determined which suggestions were accepted, corrected package and dependency mismatches, ran the build and tests, validated runtime behavior, and documented remaining risks.

The chronological record is maintained in [ai-usage/AI_USAGE_LOG.md](ai-usage/AI_USAGE_LOG.md). The attestation is in [ATTESTATION.md](ATTESTATION.md).

## Limitations

- Concurrent append serialization still needs a database locking or chain-head strategy.
- Application-level append-only APIs do not prevent a fully privileged database administrator from rewriting records and hashes.
- Production should use external chain anchoring, signatures, WORM storage, or an immutable ledger.
- Redaction hides fields from API responses but retains original payload data internally in the prototype.
- Export processing is currently unpaged and its hash does not prove global completeness without additional proof.
- Scenario C is an assignment-level interpretation, not a legal compliance certification.
- In-memory HTTP Basic users are suitable for the prototype only; production requires managed identity, TLS, MFA, and fine-grained authorization.
- Local `ddl-auto=update` should be replaced with versioned migrations before deployment.

See [docs/10-risks-tradeoffs-limitations.md](docs/10-risks-tradeoffs-limitations.md) and [docs/12-security-design.md](docs/12-security-design.md) for the full analysis.

## Live Defense Notes

The recommended defense flow is:

1. Explain the ambiguous requirements and chosen assumptions.
2. Show the package and component architecture.
3. Append two events and explain the genesis and previous-hash linkage.
4. Query the events through Swagger.
5. Run `/api/audit/verify` on the intact chain.
6. Execute the SQL tamper demonstration and show the first inconsistency.
7. Explain that redaction changes the API representation but not the immutable hash source.
8. Explain soft archival, export boundaries, compliance scope, and security roles.
9. State production limitations honestly: concurrency, database privilege, external anchoring, migrations, and identity management.

See [docs/11-live-defence-notes.md](docs/11-live-defence-notes.md) for additional preparation material.