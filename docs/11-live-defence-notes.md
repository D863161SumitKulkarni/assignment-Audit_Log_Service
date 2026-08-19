# Live Defense Notes

## Five-Minute Walkthrough

1. Start PostgreSQL and the service with `DB_PASSWORD`, `AUDIT_ADMIN_PASSWORD`, and `AUDIT_AUDITOR_PASSWORD` set.
2. Open Swagger at `http://localhost:8080/swagger-ui/index.html`.
3. Explain that the service is backend-only and the public audit API is append-only.
4. Use admin credentials to append two events and show the server timestamp, genesis hash, and next-event linkage.
5. Use auditor credentials to query by actor, resource, event type, and time range.
6. Run `GET /api/audit/verify` and explain the first-failure response contract.
7. Run `database/tamper-test.sql` against a local test database and run verification again.
8. Show `chainIntact=false` and `CURRENT_HASH_MISMATCH` without treating the broken chain as an HTTP server error.

## Scenario B Talking Points

- Retention is soft archival: no physical deletion and archived records remain in verification.
- Redaction changes the response representation, not `payload_original`, `previousHash`, or `currentHash`.
- Repeated and nested redaction are supported by the current path-aware implementation.
- Exports are bounded and commit ordered event IDs, record hashes, and chain boundaries in the export hash.

## Scenario C Talking Points

- The original compliance statement was ambiguous.
- The prototype normalizes it to `CLIENT_ACCOUNT` with four selected access event types.
- Compliance reports support actor, client account, time range, pagination, and explicit archived-record inclusion.
- This is traceability reporting, not a legal certification or complete regulator authorization model.

## Security Talking Points

- Swagger/OpenAPI is public for review.
- Audit reads require `AUDITOR` or `ADMIN`.
- Appends, redaction, and retention require `ADMIN`.
- HTTP Basic and in-memory users are prototype controls; production requires external identity, TLS, MFA, and secret management.

## Design Decisions to Defend

- Server-assigned timestamps prevent caller backdating.
- SHA-256 is deterministic and easy to independently reproduce, but it is tamper-evidence rather than tamper-prevention.
- PostgreSQL advisory locking serializes append transactions across application instances.
- PostgreSQL JSONB is parsed and re-canonicalized during verification to avoid whitespace formatting mismatches.
- External anchoring, WORM storage, signatures, and immutable ledgers are production hardening options.

## Expected Live Questions

| Question | Answer direction |
| --- | --- |
| What happens if two writers append simultaneously? | A transaction-scoped PostgreSQL advisory lock serializes the chain-head read and save; a real contention integration test remains a validation item. |
| Can a database administrator rewrite every hash? | Yes. A fully privileged administrator can defeat an internal chain; external anchoring and restricted database controls are required for stronger assurance. |
| Why does redaction not break verification? | Verification uses canonicalized `payload_original`; `payload_redacted` is only the response representation. |
| Does an export prove the complete global audit history? | No. It proves the selected bundle metadata and commitments; global completeness needs a source checkpoint or range proof. |
| What remains out of scope? | External identity, MFA, TLS deployment, legal compliance certification, migration operations, and immutable storage. |

## Final Defense Checklist

- [ ] `mvn clean test` passes.
- [ ] Swagger UI loads.
- [ ] Admin append returns 201.
- [ ] Auditor append returns 403.
- [ ] Auditor query returns 200.
- [ ] Intact verification returns `chainIntact=true` on a clean database.
- [ ] Tamper script produces `CURRENT_HASH_MISMATCH`.
- [ ] Scenario B and C boundaries are explained honestly.
- [ ] AI usage log explains accepted, modified, and rejected assistance.
