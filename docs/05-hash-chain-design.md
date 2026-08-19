# Hash Chain Design

## 1. Goal

The hash chain makes unauthorized changes to audit records detectable. Each record commits to its own immutable event data and to the preceding record's hash. A change to a payload, event field, timestamp, link, or stored hash should cause verification to report the first affected record.

The design provides tamper evidence, not absolute proof that the database has never been modified. Its strength depends on protecting the database, application credentials, and any external integrity anchor.

## 2. Fields Included in `currentHash`

The current implementation creates a deterministic hash input in this order:

```text
eventType|actorId|resourceType|resourceId|payloadCanonicalJson|eventTimestamp.toString()|previousHash
```

The fields are:

1. `eventType`
2. `actorId`
3. `resourceType`
4. `resourceId`
5. Canonical JSON representation of the original payload
6. `eventTimestamp.toString()` in ISO-8601 form
7. `previousHash`

The pipe character is the field delimiter. All fields are required by the hash service and null inputs are rejected with `IllegalArgumentException`.

The SHA-256 digest is encoded as a 64-character lowercase hexadecimal string and stored as `currentHash`.

### Canonical JSON handling

The payload is serialized with Jackson using stable map-entry ordering. Because PostgreSQL JSONB may normalize whitespace when it stores a value, verification parses `payload_original` and serializes it again before recalculating the hash. This ensures semantically identical JSON is hashed consistently rather than allowing database formatting to create a false mismatch.

Null values are preserved by the JSON utility. They are not silently excluded from the committed representation.

## 3. `previousHash` Behavior

`previousHash` links the current record to the record immediately before it in ascending database ID order.

- The first record points to the configured genesis value.
- Every later record points to the previous record's `currentHash`.
- The current record's `previousHash` is included in its own `currentHash` calculation.
- Changing a previous link breaks the relationship with the preceding record and changes the current record's expected hash.
- Verification stops at the first inconsistency and reports that record.

This creates a sequential chain rather than an independently verifiable set of unrelated records.

## 4. Genesis Hash Value

The first record uses a genesis hash consisting of 64 zero characters:

```text
0000000000000000000000000000000000000000000000000000000000000000
```

The length matches the lowercase hexadecimal representation of a SHA-256 digest. The genesis value is a protocol constant, not a hash of an event.

## 5. SHA-256 Rationale

SHA-256 was selected because it is:

- Widely implemented in Java and PostgreSQL ecosystems.
- Deterministic for the same byte sequence.
- Fast enough for the assignment's audit-event workload.
- Resistant to practical preimage and collision attacks for this use case.
- Easy for reviewers to reproduce and validate independently.
- Representable as a compact 64-character hexadecimal value.

SHA-256 provides tamper detection when an attacker cannot also replace every trusted copy of the hashes. It does not provide digital signatures, identity proof, or an external immutable checkpoint by itself.

## 6. Server-Assigned Timestamp Rationale

The service assigns `eventTimestamp` with the server clock rather than accepting it from the caller. This prevents a caller from submitting a backdated event and hiding its position in a historical query.

The server timestamp is included in the hash input, so changing it after persistence also causes a `CURRENT_HASH_MISMATCH` during verification.

The application assumes the server clock is sufficiently synchronized. A production system should use monitored time synchronization and define the authoritative timezone and clock policy.

## 7. Verification Algorithm

`GET /api/audit/verify` performs these steps:

1. Load all audit records ordered by ascending database `id`.
2. Initialize `expectedPreviousHash` with the 64-zero genesis hash.
3. Set `checkedRecords` to zero.
4. For each record:
   1. Increment `checkedRecords`.
   2. Compare the record's `previousHash` with `expectedPreviousHash`.
   3. If they differ, return a broken result with `PREVIOUS_HASH_MISMATCH` and stop.
   4. Parse the stored `payload_original` JSONB value.
   5. Re-serialize the parsed payload into canonical JSON.
   6. Recalculate the expected current hash from the immutable event fields, canonical original payload, timestamp, and stored previous hash.
   7. Compare the recalculated value with the record's `currentHash`.
   8. If they differ, return a broken result with `CURRENT_HASH_MISMATCH` and stop.
   9. Set `expectedPreviousHash` to the record's stored `currentHash`.
5. If every record passes, return `chainIntact=true` and the number of checked records.

A broken chain is a valid verification result, so the endpoint returns HTTP 200 for both intact and broken chains. The response identifies the first broken event ID, database ID, violation type, expected value, actual value, and message.

## 8. Violation Types

### `PREVIOUS_HASH_MISMATCH`

The record's stored `previousHash` does not equal the hash expected from the genesis value or the preceding record's `currentHash`.

Typical causes include:

- A record was inserted or removed from the sequence.
- A previous link was directly modified.
- Records were restored in the wrong order.
- A chain fork was created by concurrent append operations.

### `CURRENT_HASH_MISMATCH`

The record's stored `currentHash` does not equal the SHA-256 value recalculated from the record's committed fields.

Typical causes include:

- `payload_original` was changed.
- An actor, resource, event type, or timestamp was changed.
- The record's `previousHash` was changed without recalculating its current hash.
- The stored current hash itself was changed.
- The canonicalization contract changed between writing and verification.

## 9. Why Direct Database Tampering Is Detected

The public API does not expose update or delete operations, but a privileged database user could still attempt direct SQL changes. The verifier detects changes because the stored hash no longer matches the deterministic digest of the stored event fields.

For example, changing `payload_original` without changing `current_hash` leaves the old commitment in place. Verification recalculates the hash from the changed payload and returns:

```json
{
  "chainIntact": false,
  "violationType": "CURRENT_HASH_MISMATCH"
}
```

Changing a link produces a previous-link failure or a current-hash failure. Inserting, removing, or reordering records can break the expected sequence and cause a first inconsistency to be reported.

The reproducible local demonstration is documented in `database/tamper-test.sql`.

## 10. Limitations

- A fully malicious database administrator can modify every event field, every `previousHash`, every `currentHash`, and the verification code or database itself. Hash chaining cannot defend against an attacker who controls all trusted inputs.
- A database administrator can potentially remove the final records and make the remaining prefix appear internally consistent unless an external checkpoint records the expected chain head or record count.
- The current delimiter-based input requires a stable field contract. A production design should use canonical structured fields, explicit field names, or length framing to eliminate delimiter ambiguity.
- Concurrent appends require serialization. Without a database lock or protected chain-head record, concurrent writers can read the same latest hash and create a fork.
- SHA-256 detects changes but does not prove who made them or when a malicious modification occurred.
- JSONB semantic canonicalization must remain stable across library and configuration changes.

A stronger production system would combine the chain with one or more of:

- External anchoring of periodic chain-head hashes.
- Digital signatures and managed signing keys.
- WORM storage or immutable object-lock retention.
- An immutable ledger or append-only ledger service.
- Independent audit checkpoints and replicated evidence stores.
- Restricted database roles and database-level update/delete protections.

## 11. Archived and Redacted Records

### Archived records

Archival changes only lifecycle metadata:

- `archived` becomes `true`.
- `archivedAt` records when archival occurred.

The service does not change `payload_original`, `previousHash`, `currentHash`, or `eventTimestamp`. Archived records therefore remain in their original chain position and are always included by chain verification, even when ordinary queries exclude them by default.

### Redacted records

Redaction creates a separate response representation:

- The original payload remains unchanged as the integrity source.
- `payload_redacted` stores the response-safe representation.
- `redacted`, `redactedAt`, and `redactionReason` record the controlled operation.
- The mapper returns the redacted payload for normal responses when available.
- Chain verification always canonicalizes and hashes `payload_original`, never `payload_redacted`.

Consequently, users can receive a safer response payload without changing the historical commitment that proves the original audit event has not been altered.

## Summary

The design combines server-assigned event timestamps, canonical JSON, a genesis-linked SHA-256 chain, immutable original payload commitments, and first-failure verification. It is suitable for demonstrating tamper evidence in the assignment and makes the boundary between prototype integrity and production-grade immutability explicit.
