# OAuth2 and JWT Security

## Purpose

The audit service now acts as an OAuth2 Resource Server. An external identity provider authenticates users and issues signed JWT access tokens. The API validates those tokens and uses their roles to authorize each endpoint.

This replaces the previous prototype-only in-memory users and HTTP Basic authentication.

## Terminology


JWT alone is not the complete authentication system. The secure architecture is an OIDC/OAuth2 identity provider issuing JWT access tokens to this OAuth2 resource server.

## Request Flow

```text
User or client
    |
    | 1. Authenticate with the identity provider
    v
Identity provider
    |
    | 2. Return signed JWT access token
    v
Client
    |
    | 3. Authorization: Bearer <token>
    v
Audit Log API
    |
    | 4. Validate signature, issuer, expiry, and claims
    | 5. Convert roles to Spring authorities
    v
Controller and service
```

The API does not receive or store the user's password. It trusts only tokens signed by the configured issuer for the configured audience and rejects missing, invalid, expired, incorrectly signed, or incorrectly targeted tokens.

## JWT Configuration

The issuer is configured in `application.properties`:

```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=${OAUTH2_ISSUER_URI:http://localhost:8081/realms/audit}
spring.security.oauth2.resourceserver.jwt.audiences=${OAUTH2_AUDIENCE:audit-log-service}
```

Set `OAUTH2_ISSUER_URI` to the issuer URL and `OAUTH2_AUDIENCE` to the API audience configured in the selected identity provider. Spring uses the issuer metadata to discover the provider's public signing keys and validates the token's signature, issuer, expiry, and audience.

An example token for an administrator is:

```json
{
  "sub": "admin-123",
  "iss": "https://identity.example.com/realms/audit",
  "aud": "audit-log-service",
  "roles": ["ADMIN", "AUDITOR"],
  "exp": 1787150000
}
```

The client sends it on every protected request:

```http
Authorization: Bearer <access-token>
```

Bearer tokens must only be sent over HTTPS.

## How Authorization Works

`SecurityConfig` reads the JWT `roles` claim. Each role is converted as follows:

```text
AUDITOR -> ROLE_AUDITOR
ADMIN   -> ROLE_ADMIN
```

Spring's `hasRole("AUDITOR")` and `hasRole("ADMIN")` expressions then compare those authorities with the endpoint policy.

| Endpoint | Required role | Reason |
| --- | --- | --- |
| Swagger UI and OpenAPI JSON | Public | API contract review only |
| `GET /api/audit/events` | `AUDITOR` or `ADMIN` | Read audit records |
| `GET /api/audit/verify` | `AUDITOR` or `ADMIN` | Verify chain integrity |
| `GET /api/audit/export/**` | `AUDITOR` or `ADMIN` | Export evidence |
| `GET /api/audit/compliance/**` | `AUDITOR` or `ADMIN` | Read compliance reports |
| `POST /api/audit/events` | `ADMIN` | Append to the chain |
| `POST /api/audit/events/{eventId}/redact` | `ADMIN` | Change redaction metadata |
| `POST /api/audit/retention/archive` | `ADMIN` | Change archival metadata |

An unauthenticated request is rejected with `401 Unauthorized`. A valid token without the required role is rejected with `403 Forbidden`. A valid token with the required role is allowed to continue to controller validation and business logic.

## Current Spring Security Design

The API is stateless. Sessions are not created and every request must carry its bearer token. CSRF remains disabled because the API does not use cookie-based browser authentication. If a browser session is introduced later, CSRF protection must be enabled and reviewed.

The application still exposes no update or delete API for audit events. OAuth2 authorization protects the available API operations; it does not by itself make the PostgreSQL table immutable.

## Local Setup

Run an OIDC provider such as Keycloak and create a realm named `audit`. Configure a client for this API and create users or groups that receive the `ADMIN` and `AUDITOR` roles. Then set:

```powershell
$env:OAUTH2_ISSUER_URI = "http://localhost:8081/realms/audit"
.\mvnw.cmd spring-boot:run
```

The token's `roles` claim must contain the role names expected by the API. Identity providers often place roles under a provider-specific nested claim. In that case, configure a protocol mapper or adapt the JWT converter to read that claim.

## Testing

`SecurityIntegrationTest` uses Spring Security's `jwt()` test request processor. It creates test JWTs without contacting an identity provider, allowing the authorization policy to be tested independently:


Production or staging validation should additionally test real tokens from the identity provider, including expired tokens, wrong issuers, wrong audiences, invalid signatures, and missing roles.

## Security Responsibilities

The identity provider is responsible for user authentication, MFA, account lifecycle, role assignment, and token issuance. The audit API is responsible for validating the token and enforcing endpoint authorization. The database is responsible for storage, but database-level immutability and protection against privileged SQL users remain separate controls.

## Production Requirements
