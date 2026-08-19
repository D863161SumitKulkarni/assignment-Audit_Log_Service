# Scenario B: Retention, Redaction, and Bulk Export

## 1. Overview

Scenario B extends the append-only audit log with lifecycle and evidence-handling capabilities:

- Retention archives older events without physically deleting them.
- Structured redaction produces a safer response payload while preserving the original integrity commitment.
- Bulk export returns actor- or resource-filtered records with chain metadata and an export hash.

The operations are exposed through the secured backend API. `ADMIN` is required for retention and redaction mutations. `AUDITOR` or `ADMIN` can read exports.

## 2. Retention Policy

### Soft archival

Retention is implemented as soft archival. For each event older than the requested retention window, the service sets:

- `archived=true`
- `archivedAt=<server timestamp>`

It does not physically delete the row.

### Configurable window

The endpoint accepts a `days` query parameter and defaults to 90 days:

```text
POST /api/audit/retention/archive?days=90
```

The cutoff is calculated from the server's current `Instant`. Negative values are rejected.

### No physical deletion

Retention does not modify or remove:

- `payload_original`
- `payload_redacted`
- `previousHash`
- `currentHash`
- `eventTimestamp`

The public API exposes no delete operation for audit events.

### Archived records and verification

Archived records remain part of chain verification because archival changes only lifecycle metadata. `GET /api/audit/verify` checks archived records in the same ascending chain order as active records. Normal query operations exclude archived records by default unless `includeArchived=true` is requested.

### Validation example

```powershell
$adminUser = "admin"
$adminPassword = "<local-admin-password>"

curl.exe -i -u "$adminUser`:$adminPassword" `
  -X POST `
  "http://localhost:8080/api/audit/retention/archive?days=90"
```

Expected response:

```json
{
  "archivedCount": 12,
  "message": "Audit events archived only; no records were deleted"
}
```

A negative window should return HTTP 400:

```powershell
curl.exe -i -u "$adminUser`:$adminPassword" `
  -X POST `
  "http://localhost:8080/api/audit/retention/archive?days=-1"
```

## 3. Structured Redaction

### Immutable original payload

The original payload is stored in `payload_original`. It remains the integrity source and is not overwritten by redaction. The prompt's malformed `payloadoriginal` spelling refers to this normalized field name.

Redaction records metadata separately:

- `payload_redacted`
- `redacted=true`
- `redactedAt`
- `redactionReason`

### Redacted response payload

When an event is marked redacted and `payload_redacted` is present, `AuditEventMapper` returns that payload to API clients. Otherwise, it returns the original payload.

The current prototype removes requested top-level fields from a structured JSON object. Missing top-level fields are ignored without failing the request.

### Hash chain continues from the original payload

Hash verification parses and re-canonicalizes `payload_original`. It never uses `payload_redacted` to recalculate `currentHash`.

Therefore:

- Redaction does not change `previousHash`.
- Redaction does not change `currentHash`.
- Redaction does not invalidate the historical chain.
- A client can receive a reduced payload while the service retains the original integrity evidence.

### Validation example

First obtain an event UUID from a query response, then redact selected fields:

```powershell
$eventId = "<event-uuid>"
$adminUser = "admin"
$adminPassword = "<local-admin-password>"

curl.exe -i -u "$adminUser`:$adminPassword" `
  -X POST `
  -H "Content-Type: application/json" `
  -d '{"fieldsToRedact":["email","phone"],"reason":"privacy request"}' `
  "http://localhost:8080/api/audit/events/$eventId/redact"
```

Expected response characteristics:

- HTTP `200 OK`.
- `redacted=true`.
- The response `payload` does not include the selected top-level fields.
- `currentHash` and `previousHash` remain the same as before redaction.

Verify afterward:

```powershell
$auditorUser = "auditor"
$auditorPassword = "<local-auditor-password>"

curl.exe -i -u "$auditorUser`:$auditorPassword" `
  http://localhost:8080/api/audit/verify
```

Expected result: the chain remains intact if it was intact before redaction.

### Trade-off: hidden from API but stored internally

The prototype hides selected sensitive fields from normal API responses, but `payload_original` remains stored internally so the original event can be verified. This creates a deliberate privacy trade-off:

- Benefit: the service retains strong integrity evidence and can explain the original commitment.
- Cost: a privileged database operator or compromised database can still access the original sensitive data.

This behavior must be clearly disclosed to data owners and security reviewers.

### Production alternatives

A production privacy design could use:

- Encryption of sensitive payload fields with separate keys.
- Key destruction or cryptographic erasure when policy requires unrecoverability.
- Field-level access controls and separate protected storage.
- Cryptographic commitments that prove a value existed without retaining the plaintext.
- WORM or immutable evidence storage for the commitment and audit metadata.

These alternatives require legal, key-management, recovery, and operational decisions beyond the prototype.

## 4. Bulk Export

### Export by `actorId`

```text
GET /api/audit/export/actor/{actorId}
```

The service loads matching records ordered by ascending database ID and maps them to `AuditEventResponse` records.

Example:

```powershell
$auditorUser = "auditor"
$auditorPassword = "<local-auditor-password>"

curl.exe -i -u "$auditorUser`:$auditorPassword" `
  "http://localhost:8080/api/audit/export/actor/scenario-a-user"
```

### Export by `resourceId`

```text
GET /api/audit/export/resource/{resourceId}
```

Example:

```powershell
curl.exe -i -u "$auditorUser`:$auditorPassword" `
  "http://localhost:8080/api/audit/export/resource/scenario-a-account"
```

The prompt's malformed `resourceld` spelling refers to the normalized `resourceId` path variable.

### Bundle contents

Each export bundle contains:

- `exportedAt`
- `filterType`
- `filterValue`
- Ordered `records`
- `firstRecordPreviousHash`
- `lastRecordCurrentHash`
- `hashAlgorithm` set to `SHA-256`
- `exportHash`

Example response shape:

```json
{
  "exportedAt": "2026-08-19T11:00:00Z",
  "filterType": "actorId",
  "filterValue": "scenario-a-user",
  "records": [],
  "firstRecordPreviousHash": null,
  "lastRecordCurrentHash": null,
  "hashAlgorithm": "SHA-256",
  "exportHash": "<sha-256-export-metadata-hash>"
}
```

Empty exports return HTTP 200, an empty record list, null chain boundaries, and a deterministic hash over the empty bundle metadata.

### Independent verification concept

A reviewer can independently inspect an export by:

1. Confirming the filter metadata and record order.
2. Confirming that the first record's `previousHash` and final record's `currentHash` match the bundle boundaries.
3. Reconstructing the canonical export metadata containing the filter, ordered event IDs, and boundaries.
4. Recalculating `exportHash` with SHA-256.
5. Calling `GET /api/audit/verify` against the source service to validate the complete chain.

The current export hash commits to event IDs and boundary hashes. It does not yet commit every middle record's complete canonical content, so a stronger production export should include every record hash or a Merkle-root-style commitment.

## 5. Limitations

- Retention uses Hibernate schema updates for the local prototype rather than versioned migrations.
- There is no legal-hold model or regulation-specific retention policy.
- Redaction currently handles top-level fields, not nested JSON paths.
- Repeated redaction is not yet modeled as a fully cumulative policy workflow.
- Original sensitive data remains in PostgreSQL after redaction.
- Export uses an unpaged query and may load many records into memory.
- Export integrity currently covers metadata, ordered event IDs, and chain boundaries rather than all record contents.
- No database trigger or restricted role prevents a privileged SQL user from modifying or deleting rows.
- Concurrent append serialization is still a production hardening requirement.
- Authentication is prototype HTTP Basic with in-memory users, not an external identity platform.

## 6. Future Hardening

- Replace `ddl-auto=update` with Flyway or Liquibase migrations.
- Add legal holds, policy versions, retention audit events, and explicit archive scopes.
- Implement recursive, policy-driven, cumulative redaction with sensitive-field classification.
- Encrypt original payloads with managed keys and define cryptographic erasure procedures.
- Stream exports in bounded chunks and impose maximum export sizes.
- Include every record's committed hash or canonical content in the export commitment.
- Add detached signatures, external chain-head anchoring, WORM storage, or an immutable ledger.
- Serialize concurrent appends with a database lock or protected chain-head record.
- Add database-level append-only controls, restricted SQL roles, and monitoring for direct tampering.
- Replace in-memory HTTP Basic users with an external identity provider, MFA, token rotation, and fine-grained authorization.
- Add integration tests for retention, nested/repeated redaction, export verification, large exports, and database tampering.
