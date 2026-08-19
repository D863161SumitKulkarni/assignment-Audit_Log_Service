# Security Design

## Purpose

This document records the security controls added to the audit log service, why they were selected, and the boundaries that remain for a production deployment.

## Threats Considered

The service handles audit payloads, actor identifiers, resource identifiers, hash metadata, redaction state, retention state, exports, and compliance reports. The primary risks are:

- Unauthenticated users reading or modifying audit data.
- Auditors receiving mutation privileges by mistake.
- Export or compliance endpoints exposing more data than intended.
- Credentials being committed to source control.
- Cross-site request forgery affecting a browser-authenticated stateful session.
- Malformed client input being reported as an internal server error.

## Controls Implemented

### Spring Security

The project uses `spring-boot-starter-security` with a stateless HTTP Basic security filter chain. HTTP Basic is intentionally simple for this interview prototype and makes Swagger testing straightforward. Production deployments should prefer an external identity provider and short-lived tokens.

### Authentication

Two in-memory users are configured for the prototype:

- `admin`: receives `ADMIN` and `AUDITOR` roles.
- `auditor`: receives the `AUDITOR` role.

Usernames and passwords are read from environment-backed properties:

- `AUDIT_ADMIN_USERNAME`
- `AUDIT_ADMIN_PASSWORD`
- `AUDIT_AUDITOR_USERNAME`
- `AUDIT_AUDITOR_PASSWORD`

The defaults are local-development placeholders only. They must be replaced before sharing or deploying the application.

### Authorization

| Resource | Required role | Reason |
| --- | --- | --- |
| Swagger UI and OpenAPI JSON | Public | Allows API contract review without exposing business data. |
| `GET /api/audit/events` | `AUDITOR` or `ADMIN` | Read-only audit investigation. |
| `GET /api/audit/verify` | `AUDITOR` or `ADMIN` | Integrity verification is read-only. |
| `GET /api/audit/export/**` | `AUDITOR` or `ADMIN` | Controlled audit export. |
| `GET /api/audit/compliance/**` | `AUDITOR` or `ADMIN` | Compliance investigation and reporting. |
| `POST /api/audit/events` | `ADMIN` | Appending affects the integrity chain. |
| `POST /api/audit/events/{eventId}/redact` | `ADMIN` | Redaction changes the response representation and metadata. |
| `POST /api/audit/retention/archive` | `ADMIN` | Retention changes archival state. |

No update or delete API is exposed.

### Session and CSRF behavior

Sessions are disabled with `STATELESS`, so each request must carry its credentials. CSRF is disabled because the API is stateless and uses HTTP Basic rather than a browser session or cookie-based authentication. If authentication changes to cookies or a browser session, CSRF protection must be enabled and reviewed again.

### Input error handling

Malformed timestamps, malformed request bodies, and invalid page constraints are returned as HTTP 400 responses. This avoids incorrectly classifying client input errors as HTTP 500 server failures.

### Request bounds

Audit and compliance query endpoints require page numbers of at least zero, page sizes of at least one, and page sizes no greater than 100. This limits accidental unbounded queries and reduces memory and database pressure.

### Secret handling

The database password is no longer hard-coded in `application.properties`; it is read from `DB_PASSWORD`. The local development command must set that variable, for example in PowerShell:

```powershell
$env:DB_PASSWORD = "<local-password>"
.\mvnw.cmd spring-boot:run
```

Do not commit the value of `DB_PASSWORD` or any audit security password.

## Why This Design

- It is small enough to review during an interview assignment.
- It distinguishes read-only auditor access from administrative mutation access.
- It keeps Swagger available for API review while protecting audit data.
- It avoids a persistent user database and external identity provider that the assignment does not require.
- It makes the security assumptions explicit instead of implying production-grade identity management.

## Production Gaps

This prototype does not yet provide:

- An external identity provider, MFA, token rotation, or account lifecycle management.
- Database row-level security, tenant isolation, or resource-level authorization.
- Encrypted secrets management, TLS termination, or certificate rotation.
- Rate limiting, lockout, anomaly detection, or security event monitoring.
- Cryptographic signing or external notarization of the hash chain.
- Separate regulator permissions beyond the `AUDITOR` role.
- Database-level prevention of direct updates, deletes, truncation, or privileged SQL tampering.

These gaps are documented intentionally and must be addressed before production use.
