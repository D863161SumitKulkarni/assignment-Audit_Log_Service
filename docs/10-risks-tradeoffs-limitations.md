# Risks, Trade-offs, and Limitations

## 1. Tamper-Evidence Versus Tamper-Prevention

The audit log provides tamper evidence: it makes many unauthorized changes detectable by recalculating the hash chain and reporting the first inconsistency. It does not, by itself, prevent a database operator or compromised administrator from changing stored data.

The append-only public API prevents ordinary clients from requesting update or delete operations, but API design is not equivalent to database immutability. A privileged SQL user may still bypass the application unless database roles, triggers, procedures, or immutable storage controls are added.

## 2. Privileged Database Administrator Risk

A fully privileged database administrator may be able to modify:

- Event payloads and business fields.
- `previousHash` and `currentHash` values.
- Timestamps and lifecycle metadata.
- The verification code, database schema, or database backups.
- The final records or chain history itself.

If the same attacker can alter every record and every trusted verifier, an internal hash chain cannot prove that the data was not rewritten. This is a fundamental trust-boundary limitation, not a defect that another local hash can solve.

Current mitigation is limited to application-level append-only endpoints, verification, security roles, and documented tamper testing.

## 3. Need for External Anchoring in Production

A production system should periodically publish a chain-head commitment outside the primary database. Suitable options include:

- A separately controlled evidence store.
- A signed checkpoint service.
- WORM or object-lock storage.
- A public or regulated timestamping service.
- An immutable ledger.

An external anchor provides an independent historical reference. If the database is later rewritten, the rewritten chain head can be compared with the previously anchored commitment.

## 4. SHA-256 Choice and Limitations

SHA-256 was selected because it is standardized, widely available, deterministic, fast, and straightforward to reproduce in Java, PostgreSQL tooling, and independent verification scripts.

The limitations are:

- SHA-256 detects changes but does not identify who made them.
- It does not provide non-repudiation or a legal signature.
- It depends on a stable canonicalization and field-order contract.
- Delimiter-based input requires careful handling of delimiters inside field values.
- It cannot protect data from an administrator who can rewrite all hashes and trusted verification inputs.

A production implementation should use canonical structured hash input, explicit field names or length framing, key-backed signatures where appropriate, and external anchoring.

## 5. Server-Assigned Timestamp Trade-off

The service assigns `eventTimestamp` on the server rather than trusting a caller-provided timestamp. This prevents clients from backdating events and makes the recorded event chronology part of the service's integrity boundary.

Trade-offs include:

- The server clock becomes authoritative.
- Clock drift can affect ordering and time-range interpretation.
- Distributed deployments need time synchronization and a documented clock policy.
- The timestamp reflects service receipt time, which may differ from the time an upstream business action began.

Production hardening should include synchronized clocks, monitoring, timezone policy, and clear distinction between event occurrence time and ingestion time if both are required.

## 6. Redaction Trade-off

### Current behavior

API redaction works by creating `payloadRedacted` and returning that representation to API clients when the event is marked redacted. The immutable original payload remains available internally, and chain verification continues to use the original payload rather than the redacted response.

This preserves integrity evidence while reducing exposure through ordinary API responses.

### Privacy limitation

The original data remains stored internally for verification. A privileged database user, backup operator, or compromised storage system may still access it. Redaction is therefore response-level protection, not cryptographic erasure.

### Stronger production options

A production design could use:

- Field-level encryption with separately managed keys.
- Key destruction or cryptographic erasure when policy requires data to become unrecoverable.
- Cryptographic commitments that prove a value existed without retaining plaintext.
- Separate restricted storage for original sensitive payloads.
- Policy-driven redaction with nested paths and cumulative operations.

These options introduce key recovery, legal hold, operational, and auditability trade-offs.

## 7. Retention Trade-off

Retention uses archive metadata rather than physical deletion:

- Events older than the configured window become `archived=true`.
- `archivedAt` records when the state changed.
- Original payloads, hashes, and event timestamps remain unchanged.
- Archived records remain available to chain verification.
- Ordinary queries exclude archived events by default.

This preserves historical verification and avoids breaking the chain, but it means storage continues to grow. It also may conflict with legal deletion requirements or data-subject erasure obligations. Production policy must define legal holds, archive access, retention schedules, and whether some data needs separate cryptographic erasure.

## 8. Export Limitations

The export API verifies the integrity of the exported bundle metadata as follows:

- Records are ordered by database ID.
- The bundle includes the first previous hash and last current hash.
- The bundle includes the hash algorithm.
- The export hash commits to filter metadata, ordered event IDs, and chain boundaries.

This provides useful evidence that the returned bundle has not been casually rearranged or its boundary metadata changed. It does not prove global completeness by itself. A filtered export may omit events outside its filter, and the current export hash does not commit every middle record's complete canonical content.

Stronger completeness evidence would include every record's committed hash, a Merkle root, a signed manifest, a source-chain checkpoint, or an independently verifiable range proof.

## 9. Compliance Reporting Scope Limitations

Scenario C is implemented as an assignment-level interpretation:

- `resourceType=CLIENT_ACCOUNT`.
- Event types: `ACCOUNT_VIEWED`, `ACCOUNT_EXPORTED`, `ACCOUNT_UPDATED`, and `PERMISSION_GRANTED`.
- Optional client account, actor, and time filters.
- Archived records excluded by default.

This does not establish legal compliance or guarantee that the event allowlist matches a specific regulation. The original requirement did not define:

- The applicable regulatory framework.
- The identity and authorization model for regulators.
- The complete client-data classification.
- The required report format or delivery process.
- The required retention period or legal hold behavior.
- Whether failed access, denied access, administrative access, or background processing count as access.

Production implementation requires stakeholder and legal clarification before the report is treated as authoritative evidence.

## 10. Operational Risks

### Concurrency during writes

The current append workflow reads the latest hash and then saves a new event. Concurrent writers may read the same chain head and create competing records with the same `previousHash`. This can fork the chain.

Mitigation should serialize appends using a locked chain-head row, PostgreSQL advisory lock, serializable transaction strategy, or an equivalent mechanism with retry handling.

### Database transaction isolation

The default transaction isolation level may not be enough to guarantee a single linear append sequence under contention. Isolation, locking, retry, and failure semantics must be tested against PostgreSQL rather than assumed from unit tests.

### Full-chain verification performance

Verification currently loads and checks the complete chain. This is simple and transparent, but latency and memory use grow with the number of records. Large histories may require:

- Streaming verification.
- Range-based verification.
- Periodic checkpoints.
- Parallel verification of independently anchored segments.
- A maintained chain-head or Merkle summary.

### Additional operational concerns

- Unpaged exports can consume substantial memory.
- Database backups need integrity and access controls.
- Schema updates should use migrations rather than `ddl-auto=update`.
- Security credentials, TLS, monitoring, and rate limiting need operational ownership.
- Page serialization should use a stable response contract instead of relying indefinitely on `PageImpl` serialization.

## 11. Future Improvements

1. Add serialized append writes and a PostgreSQL concurrency integration test.
2. Enforce database-level append-only controls with roles, triggers, or write-only procedures.
3. Add external chain-head anchoring and signed checkpoints.
4. Replace delimiter hashing with canonical structured input and length framing.
5. Add signatures or managed signing keys for non-repudiation.
6. Encrypt sensitive original payloads and define key destruction policies.
7. Implement nested, cumulative, policy-driven redaction.
8. Stream and bound export processing.
9. Include every record commitment or a Merkle proof in export bundles.
10. Replace in-memory HTTP Basic users with an external identity provider, MFA, token rotation, and fine-grained authorization.
11. Introduce Flyway or Liquibase migrations and environment-specific database policies.
12. Add compliance governance for event taxonomy, report formats, retention, legal holds, and regulator permissions.
13. Add comprehensive controller, PostgreSQL integration, concurrency, export, and scale tests.

## Conclusion

The prototype makes meaningful tampering detectable and keeps the chain verifiable across archival and API redaction. It intentionally does not claim tamper prevention, legal compliance, non-repudiation, or production-grade immutability. Those guarantees require controls outside a local SHA-256 chain, especially external anchoring, protected storage, database governance, managed identity, and operational monitoring.
