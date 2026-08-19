# Testing Strategy

## Purpose

The testing strategy validates the audit log service at several boundaries: deterministic utility behavior, business services, Spring application wiring, secured HTTP endpoints, manual API workflows, and direct PostgreSQL tamper detection.

The key quality objective is not only to prove that requests succeed, but to prove that the audit chain remains verifiable when lifecycle operations such as archival and redaction occur.

## 1. Unit Tests

Unit tests isolate a single service or utility with no HTTP server and no production database connection.

### Current unit coverage

- `HashServiceTest`
  - SHA-256 output length and lowercase hexadecimal format.
  - Same input produces the same output.
  - Different inputs produce different outputs.
  - Changing `previousHash` changes the event hash.
  - Null input is rejected.
- `AuditEventServiceTest`
  - Genesis hash behavior.
  - Latest hash linkage.
  - Generated current hash persistence.
  - Server-assigned timestamp.
  - No repository update/delete calls from the append workflow.
- `ChainVerificationServiceTest`
  - Empty chain.
  - Valid two-record chain.
  - Previous-link mismatch.
  - Current-hash mismatch.
  - Archived records included in verification.
  - Redacted records verified using the original payload.
- `RedactionServiceTest`
  - Top-level field removal.
  - Missing fields ignored.
  - Redaction metadata updates.
  - Original payload and hashes remain unchanged.
  - Missing event produces `ResourceNotFoundException`.

Unit tests use JUnit 5 and Mockito where collaborators need to be isolated.

## 2. Service Tests

Service tests verify orchestration between repositories, hashing, canonical JSON, mapping, and domain decisions.

The append workflow is checked for:

1. Server timestamp assignment.
2. Canonical payload serialization.
3. Genesis selection for the first event.
4. Latest-record lookup for subsequent events.
5. Hash calculation using the expected field sequence.
6. Persistence of the generated event.
7. Absence of update/delete operations.

The verification workflow is checked for:

1. Ascending record traversal.
2. Genesis-link comparison.
3. Previous-hash comparison.
4. Current-hash recalculation.
5. First-failure reporting.
6. Inclusion of archived records.
7. Original-payload use after redaction.

Scenario B and C service behavior should be extended with dedicated tests for retention, export, and compliance filtering as those contracts mature.

## 3. Controller and API Tests

Spring MockMvc integration tests load the application context and validate security filters, controller routing, parameter binding, validation, and response statuses.

### Current controller/API coverage

`SecurityIntegrationTest` verifies:

- Swagger/OpenAPI documentation is public.
- Unauthenticated audit queries return HTTP 401.
- An auditor can perform read-only audit queries.
- Page sizes above 100 return HTTP 400.
- Malformed timestamp parameters return HTTP 400.

The live API smoke checks additionally verified:

- Admin append returns HTTP 201.
- Auditor append returns HTTP 403.
- Chain verification returns HTTP 200 for a broken-chain result.
- Swagger UI and OpenAPI JSON return HTTP 200.

### Recommended API test matrix

Add controller tests for:

- Valid and invalid append request bodies.
- Redaction HTTP 200, HTTP 400, and HTTP 404 responses.
- Retention default days, negative days, and archival-only response.
- Actor and resource export routes, including empty bundles.
- Compliance allowlist, time filters, pagination, and archived exclusion.
- Verification response serialization for intact and broken chains.
- `from` greater than `to` once that validation rule is implemented.
- Authorization for every mutation and read-only endpoint.

## 4. Manual Validation Using curl

The reproducible Scenario A walkthrough is documented in [06-scenario-a-validation.md](06-scenario-a-validation.md). The basic sequence is:

```powershell
$env:DB_PASSWORD = "<local-postgres-password>"
$env:AUDIT_ADMIN_PASSWORD = "<local-admin-password>"
$env:AUDIT_AUDITOR_PASSWORD = "<local-auditor-password>"
.\mvnw.cmd spring-boot:run
```

Then use authenticated `curl.exe` requests to:

1. Create the first event.
2. Create the second event.
3. Query by actor.
4. Query by resource type and resource ID.
5. Query by event type.
6. Query by inclusive time range.
7. Verify the chain.
8. Exercise invalid timestamps, invalid credentials, role restrictions, and page bounds.

Expected baseline results:

| Check | Expected status |
| --- | ---: |
| Swagger UI | 200 |
| OpenAPI JSON | 200 |
| Unauthenticated audit query | 401 |
| Auditor read query | 200 |
| Admin append | 201 |
| Auditor append | 403 |
| Malformed timestamp | 400 |
| Page size above 100 | 400 |
| Verification result | 200 |

## 5. Manual PostgreSQL Tamper Test

The tamper procedure is in [database/tamper-test.sql](../database/tamper-test.sql). It:

1. Displays the first few audit records.
2. Adds a test field to `payload_original` on the first record.
3. Does not change `current_hash`.
4. Commits the direct SQL change so the API can observe it.
5. Displays the modified row.

Run it only against a local test database:

```powershell
$env:PGPASSWORD = "<local-postgres-password>"
psql -h localhost -U postgres -d auditlogdb -f ..\..\database\tamper-test.sql
Remove-Item Env:PGPASSWORD
```

Run verification again:

```powershell
curl.exe -i -u "$auditorUser`:$auditorPassword" `
  http://localhost:8080/api/audit/verify
```

Expected result:

- HTTP 200.
- `chainIntact=false`.
- `violationType=CURRENT_HASH_MISMATCH`.
- The first inconsistent event and database ID are reported.

The existing smoke row used during live validation remains inconsistent intentionally; it was not silently repaired because doing so would conceal the integrity signal.

## 6. What Is Tested

| Behavior | Current evidence |
| --- | --- |
| Hash determinism | `HashServiceTest` |
| Append-only creation | `AuditEventServiceTest`; no update/delete repository calls |
| `previousHash` linkage | `AuditEventServiceTest`, `ChainVerificationServiceTest` |
| Verification success | Empty and valid-chain tests |
| Verification failure after tampering | Chain mismatch unit tests and `database/tamper-test.sql` |
| Query filtering | Repository specifications and live query walkthrough; controller matrix still expandable |
| Pagination | Security test for maximum size and service default-sort logic |
| Retention archive behavior | Implementation and manual endpoint procedure; dedicated automated test still recommended |
| Redaction behavior | `RedactionServiceTest` and chain verification test |
| Export bundle metadata | Implementation and API documentation; dedicated automated export test still recommended |
| Compliance reporting | Implementation and API documentation; dedicated automated compliance test still recommended |
| Authentication and authorization | `SecurityIntegrationTest` and live smoke checks |
| Malformed request handling | `SecurityIntegrationTest` and global exception handler |

## 7. What Is Not Tested and Why

The following areas are not fully covered by the current automated suite:

- **Concurrent appends:** Requires a real PostgreSQL concurrency test and a serialization design such as a chain-head lock.
- **Database-level immutability:** Unit tests cannot prove that a privileged SQL user cannot update/delete rows; this requires roles, triggers, or integration DDL tests.
- **Full controller contract coverage:** The current suite focuses on security and selected edge cases; every Scenario B/C route should receive MockMvc tests.
- **Nested and cumulative redaction:** The prototype currently handles top-level fields, and nested path semantics are not yet defined.
- **Large exports:** The current export uses an unpaged read, so memory and streaming behavior need a dedicated scale test.
- **Full independent export verification:** The current export hash commits metadata, event IDs, and boundary hashes, not every middle record's full content.
- **Compliance legal correctness:** The event allowlist is an assignment interpretation, not a verified regulatory taxonomy.
- **Production identity and tenant authorization:** In-memory prototype users do not model an external identity provider, tenant boundaries, or MFA.
- **Clock drift and time synchronization:** The tests use the local server clock and do not simulate clock skew.
- **Migration compatibility:** Hibernate namespace creation is suitable for local prototype startup; versioned migration upgrades are not tested.

These gaps are documented rather than represented as passing behavior.

## 8. Quality Gates Before Submission

### Maven tests

Run from `backend/audit-log-service` with the local database password supplied securely:

```powershell
$env:DB_PASSWORD = "<local-postgres-password>"
.\mvnw.cmd clean test
```

The expected result is zero test failures and zero errors. The current verified baseline is 30 tests, 0 failures, 0 errors, and 0 skipped.

### Swagger check

Open:

```text
http://localhost:8080/swagger-ui/index.html
```

Confirm:

- Swagger UI loads.
- All eight API groups are listed.
- Security behavior is consistent with the documented roles.
- OpenAPI JSON is available at `/v3/api-docs`.

### Verify endpoint check

With an auditor or admin credential:

```powershell
curl.exe -i -u "$auditorUser`:$auditorPassword" `
  http://localhost:8080/api/audit/verify
```

Confirm that an intact chain reports `chainIntact=true`, and that a broken chain reports HTTP 200 with a specific violation type.

### Tamper SQL check

Run `database/tamper-test.sql` against a local database, then call the verify endpoint again. Confirm `chainIntact=false` and `CURRENT_HASH_MISMATCH`.

### Documentation check

Before submission, confirm that requirements, architecture, API design, hash-chain design, Scenario A/B/C validation, testing strategy, security design, risks, attestation, and AI usage traceability all describe actual behavior rather than unimplemented plans.

## 9. Known Limitations

- The service still needs serialized append writes to prevent concurrent chain forks.
- Database-level update/delete protections are not yet enforced for privileged database users.
- `ddl-auto=update` and namespace creation are local-prototype conveniences, not a migration strategy.
- Original sensitive payload data remains internally stored after redaction.
- Export processing is not streamed or bounded by a maximum record count.
- Compliance report totals currently represent returned page content size for prototype simplicity.
- Page response serialization uses Spring Data's `PageImpl` shape, which emits a stability warning.
- Mockito emits a dynamic-agent warning on the current JDK; it does not fail tests.
- Security uses in-memory HTTP Basic users and requires production replacement with managed identity, TLS, MFA, and finer-grained authorization.
