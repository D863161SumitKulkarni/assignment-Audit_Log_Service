# API Design

## API Conventions

- Base URL: `http://localhost:8080`
- Content type for JSON requests and responses: `application/json`
- Timestamps use ISO-8601 UTC values, for example `2026-08-19T10:15:30Z`.
- Protected endpoints use stateless HTTP Basic authentication.
- Swagger UI is public for review at `/swagger-ui/index.html`.
- OpenAPI JSON is public at `/v3/api-docs`.
- `AUDITOR` and `ADMIN` can use read-only investigation APIs.
- `ADMIN` is required for append, redaction, and retention mutation operations.
- Validation errors and malformed parameters return HTTP 400.
- **No update or delete API is exposed for audit events.**

## 1. POST `/api/audit/events`

### Purpose

Append a new audit event to the tamper-evident audit chain.

### Request Parameters

The event is supplied as a JSON body:

| Field | Type | Required | Rules |
| --- | --- | --- | --- |
| `eventType` | String | Yes | Non-blank; maximum 100 characters. |
| `actorId` | String | Yes | Non-blank; maximum 150 characters. |
| `resourceType` | String | Yes | Non-blank; maximum 100 characters. |
| `resourceId` | String | Yes | Non-blank; maximum 150 characters. |
| `payload` | Object | Yes | Structured JSON object. |

`eventTimestamp`, `eventId`, `createdAt`, `previousHash`, and `currentHash` are not accepted from the caller. The server assigns or calculates them.

### Sample Request

```http
POST /api/audit/events HTTP/1.1
Authorization: Basic <admin-credentials>
Content-Type: application/json

{
  "eventType": "ACCOUNT_VIEWED",
  "actorId": "user-123",
  "resourceType": "CLIENT_ACCOUNT",
  "resourceId": "account-456",
  "payload": {
    "screen": "account-summary",
    "reason": "customer-support"
  }
}
```

### Sample Response

Status: `201 Created`

```json
{
  "eventId": "8f7b8d9f-0d85-4c42-9ee4-0bc7c4b1f4cc",
  "eventType": "ACCOUNT_VIEWED",
  "actorId": "user-123",
  "resourceType": "CLIENT_ACCOUNT",
  "resourceId": "account-456",
  "payload": {
    "screen": "account-summary",
    "reason": "customer-support"
  },
  "eventTimestamp": "2026-08-19T10:15:30Z",
  "createdAt": "2026-08-19T10:15:30.020Z",
  "previousHash": "0000000000000000000000000000000000000000000000000000000000000000",
  "currentHash": "f4f6a7d42c1b5f91e0f8c9a5c3a8d4f8f1f0d2e8d4c4e2a1a4c0f8a0f6e2d1b3",
  "hashAlgorithm": "SHA-256",
  "archived": false,
  "redacted": false
}
```

### Validation Rules

- Request body must be valid JSON.
- Required string fields must not be blank.
- Payload must be present.
- The server assigns the timestamp using its clock.
- The first record uses the 64-zero genesis hash.
- Later records use the latest record's `currentHash` as `previousHash`.

### Notes

- Requires the `ADMIN` role.
- This is the only event-creation endpoint.
- The operation is append-only; no caller can supply or override chain metadata.
- Concurrent append serialization remains a production hardening requirement.

## 2. GET `/api/audit/events`

### Purpose

Query audit events using optional filters and pagination. Archived records are excluded by default.

### Request Parameters

| Parameter | Type | Required | Default | Rules |
| --- | --- | --- | --- | --- |
| `actorId` | String | No | None | Exact match. |
| `resourceType` | String | No | None | Exact match. |
| `resourceId` | String | No | None | Exact match. |
| `eventType` | String | No | None | Exact match. |
| `from` | Instant | No | None | Inclusive lower timestamp bound. |
| `to` | Instant | No | None | Inclusive upper timestamp bound. |
| `includeArchived` | Boolean | No | `false` | Set `true` to include archived records. |
| `page` | Integer | No | `0` | Must be at least 0. |
| `size` | Integer | No | `20` | Must be between 1 and 100. |

### Sample Request

```http
GET /api/audit/events?resourceType=CLIENT_ACCOUNT&eventType=ACCOUNT_VIEWED&from=2026-08-19T00:00:00Z&to=2026-08-19T23:59:59Z&page=0&size=20 HTTP/1.1
Authorization: Basic <auditor-credentials>
```

### Sample Response

Status: `200 OK`

```json
{
  "content": [
    {
      "eventId": "8f7b8d9f-0d85-4c42-9ee4-0bc7c4b1f4cc",
      "eventType": "ACCOUNT_VIEWED",
      "actorId": "user-123",
      "resourceType": "CLIENT_ACCOUNT",
      "resourceId": "account-456",
      "payload": {
        "screen": "account-summary"
      },
      "eventTimestamp": "2026-08-19T10:15:30Z",
      "createdAt": "2026-08-19T10:15:30.020Z",
      "previousHash": "0000000000000000000000000000000000000000000000000000000000000000",
      "currentHash": "f4f6a7d42c1b5f91e0f8c9a5c3a8d4f8f1f0d2e8d4c4e2a1a4c0f8a0f6e2d1b3",
      "hashAlgorithm": "SHA-256",
      "archived": false,
      "redacted": false
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "offset": 0,
    "paged": true
  },
  "totalElements": 1,
  "totalPages": 1,
  "number": 0,
  "size": 20,
  "numberOfElements": 1,
  "first": true,
  "last": true,
  "empty": false
}
```

### Validation Rules

- `page` less than 0 returns HTTP 400.
- `size` less than 1 or greater than 100 returns HTTP 400.
- Malformed `Instant` values return HTTP 400.
- Filters are exact matches except for the timestamp range.
- The default sort is database `id` ascending when no sort is provided.

### Notes

- Requires `AUDITOR` or `ADMIN`.
- A redacted event returns its redacted response payload while retaining hash metadata.
- Normal queries exclude archived records unless `includeArchived=true`.

## 3. GET `/api/audit/verify`

### Purpose

Verify every audit event in ascending database ID order and report the first chain inconsistency.

### Request Parameters

No query parameters or request body are required.

### Sample Request

```http
GET /api/audit/verify HTTP/1.1
Authorization: Basic <auditor-credentials>
```

### Sample Response: Intact Chain

Status: `200 OK`

```json
{
  "chainIntact": true,
  "checkedRecords": 125,
  "firstBrokenEventId": null,
  "firstBrokenDatabaseId": null,
  "violationType": null,
  "expectedValue": null,
  "actualValue": null,
  "message": "Audit event hash chain is intact"
}
```

### Sample Response: Broken Chain

Status: `200 OK`

```json
{
  "chainIntact": false,
  "checkedRecords": 18,
  "firstBrokenEventId": "2c5f4e9c-0d6d-4c4c-a9e7-4de8a6eb7c21",
  "firstBrokenDatabaseId": 18,
  "violationType": "CURRENT_HASH_MISMATCH",
  "expectedValue": "recalculated-sha-256-value",
  "actualValue": "stored-sha-256-value",
  "message": "Current hash does not match the recalculated event hash"
}
```

### Validation Rules

- All records, including archived records, are checked.
- The first record must reference the 64-zero genesis hash.
- A broken previous link reports `PREVIOUS_HASH_MISMATCH`.
- A recalculated hash mismatch reports `CURRENT_HASH_MISMATCH`.
- Redacted records are verified with `payload_original`.

### Notes

- Requires `AUDITOR` or `ADMIN`.
- A broken chain is a valid verification result, not a server exception; both intact and broken results return HTTP 200.

## 4. POST `/api/audit/retention/archive`

### Purpose

Soft-archive audit events older than the requested retention period without deleting them or changing their integrity inputs.

### Request Parameters

| Parameter | Type | Required | Default | Rules |
| --- | --- | --- | --- | --- |
| `days` | Integer | No | `90` | Must not be negative. |

### Sample Request

```http
POST /api/audit/retention/archive?days=90 HTTP/1.1
Authorization: Basic <admin-credentials>
```

### Sample Response

Status: `200 OK`

```json
{
  "archivedCount": 42,
  "message": "Audit events archived only; no records were deleted"
}
```

### Validation Rules

- Negative `days` returns HTTP 400.
- Matching records have `archived` set to `true` and `archivedAt` set.
- `payload_original`, `payload_redacted`, `previousHash`, `currentHash`, and `eventTimestamp` are not changed.

### Notes

- Requires `ADMIN`.
- This endpoint performs archival only; it does not physically delete records.
- Archived records remain part of chain verification.

## 5. POST `/api/audit/events/{eventId}/redact`

### Purpose

Create a controlled redacted response payload for an existing event without changing the immutable original payload or hash chain.

### Request Parameters

Path parameter:

| Parameter | Type | Required | Rules |
| --- | --- | --- | --- |
| `eventId` | UUID | Yes | Must be a valid event UUID. |

JSON body:

| Field | Type | Required | Rules |
| --- | --- | --- | --- |
| `fieldsToRedact` | Array of String | Yes | Must not be empty. |
| `reason` | String | Yes | Must not be blank. |

### Sample Request

```http
POST /api/audit/events/8f7b8d9f-0d85-4c42-9ee4-0bc7c4b1f4cc/redact HTTP/1.1
Authorization: Basic <admin-credentials>
Content-Type: application/json

{
  "fieldsToRedact": ["email", "phone"],
  "reason": "privacy request"
}
```

### Sample Response

Status: `200 OK`

```json
{
  "eventId": "8f7b8d9f-0d85-4c42-9ee4-0bc7c4b1f4cc",
  "eventType": "ACCOUNT_VIEWED",
  "actorId": "user-123",
  "resourceType": "CLIENT_ACCOUNT",
  "resourceId": "account-456",
  "payload": {
    "screen": "account-summary"
  },
  "eventTimestamp": "2026-08-19T10:15:30Z",
  "createdAt": "2026-08-19T10:15:30.020Z",
  "previousHash": "0000000000000000000000000000000000000000000000000000000000000000",
  "currentHash": "f4f6a7d42c1b5f91e0f8c9a5c3a8d4f8f1f0d2e8d4c4e2a1a4c0f8a0f6e2d1b3",
  "hashAlgorithm": "SHA-256",
  "archived": false,
  "redacted": true
}
```

### Validation Rules

- `eventId` must be a valid UUID.
- `fieldsToRedact` must not be empty.
- `reason` must not be blank.
- A missing event returns HTTP 404.
- Original payload and hash fields remain unchanged.

### Notes

- Requires `ADMIN`.
- This is controlled redaction metadata and response-payload behavior, not a general update API.
- The prototype handles top-level fields; nested path semantics require further design.

## 6. GET `/api/audit/export/actor/{actorId}`

### Purpose

Export all audit events for an actor as an ordered, self-contained verifiable bundle.

### Request Parameters

| Parameter | Type | Required | Rules |
| --- | --- | --- | --- |
| `actorId` | Path String | Yes | Must not be blank. |

### Sample Request

```http
GET /api/audit/export/actor/user-123 HTTP/1.1
Authorization: Basic <auditor-credentials>
```

### Sample Response

Status: `200 OK`

```json
{
  "exportedAt": "2026-08-19T11:00:00Z",
  "filterType": "actorId",
  "filterValue": "user-123",
  "records": [],
  "firstRecordPreviousHash": null,
  "lastRecordCurrentHash": null,
  "hashAlgorithm": "SHA-256",
  "exportHash": "8c9a1e6c0d4a6a4d1e3b3f44bb2f0a8d2c85d6d6bcf3a8c6b2c0d5e4f1a2b3c4"
}
```

### Validation Rules

- Blank or missing actor IDs return a client error.
- Matching records are ordered by database ID ascending.
- Empty results return HTTP 200 with an empty list and null chain boundaries.
- The export hash includes filter metadata, ordered event IDs, and chain boundaries.

### Notes

- Requires `AUDITOR` or `ADMIN`.
- Large exports currently use an unpaged repository read; streaming and maximum export limits are production improvements.

## 7. GET `/api/audit/export/resource/{resourceId}`

### Purpose

Export all audit events for a resource as an ordered, self-contained verifiable bundle.

### Request Parameters

| Parameter | Type | Required | Rules |
| --- | --- | --- | --- |
| `resourceId` | Path String | Yes | Must not be blank. |

### Sample Request

```http
GET /api/audit/export/resource/account-456 HTTP/1.1
Authorization: Basic <auditor-credentials>
```

### Sample Response

Status: `200 OK`

```json
{
  "exportedAt": "2026-08-19T11:05:00Z",
  "filterType": "resourceId",
  "filterValue": "account-456",
  "records": [
    {
      "eventId": "8f7b8d9f-0d85-4c42-9ee4-0bc7c4b1f4cc",
      "eventType": "ACCOUNT_VIEWED",
      "actorId": "user-123",
      "resourceType": "CLIENT_ACCOUNT",
      "resourceId": "account-456",
      "payload": {
        "screen": "account-summary"
      },
      "eventTimestamp": "2026-08-19T10:15:30Z",
      "createdAt": "2026-08-19T10:15:30.020Z",
      "previousHash": "0000000000000000000000000000000000000000000000000000000000000000",
      "currentHash": "f4f6a7d42c1b5f91e0f8c9a5c3a8d4f8f1f0d2e8d4c4e2a1a4c0f8a0f6e2d1b3",
      "hashAlgorithm": "SHA-256",
      "archived": false,
      "redacted": false
    }
  ],
  "firstRecordPreviousHash": "0000000000000000000000000000000000000000000000000000000000000000",
  "lastRecordCurrentHash": "f4f6a7d42c1b5f91e0f8c9a5c3a8d4f8f1f0d2e8d4c4e2a1a4c0f8a0f6e2d1b3",
  "hashAlgorithm": "SHA-256",
  "exportHash": "1e6d6f9c9d3f4e3b2c1a0f9e8d7c6b5a493827161514131211100f0e0d0c0b0a09"
}
```

### Validation Rules

- Blank or missing resource IDs return a client error.
- Records are ordered by database ID ascending.
- Empty results return HTTP 200 with null chain boundaries.
- Redacted response payloads do not change the original chain commitments.

### Notes

- Requires `AUDITOR` or `ADMIN`.
- The bundle is filtered by resource ID and is not necessarily a complete chain from genesis.

## 8. GET `/api/audit/compliance/client-account-access`

### Purpose

Return the clarified Scenario C compliance report for access to client account data.

### Request Parameters

| Parameter | Type | Required | Default | Rules |
| --- | --- | --- | --- | --- |
| `clientAccountId` | String | No | None | Exact match against `resourceId`. |
| `actorId` | String | No | None | Exact match against `actorId`. |
| `from` | Instant | No | None | Inclusive event timestamp lower bound. |
| `to` | Instant | No | None | Inclusive event timestamp upper bound. |
| `page` | Integer | No | `0` | Must be at least 0. |
| `size` | Integer | No | `20` | Must be between 1 and 100. |

The service always filters `resourceType=CLIENT_ACCOUNT` and includes only `ACCOUNT_VIEWED`, `ACCOUNT_EXPORTED`, `ACCOUNT_UPDATED`, and `PERMISSION_GRANTED` events. Archived records are excluded by default in the current prototype.

### Sample Request

```http
GET /api/audit/compliance/client-account-access?clientAccountId=account-456&from=2026-08-19T00:00:00Z&page=0&size=20 HTTP/1.1
Authorization: Basic <auditor-credentials>
```

### Sample Response

Status: `200 OK`

```json
{
  "generatedAt": "2026-08-19T11:10:00Z",
  "clientAccountId": "account-456",
  "totalRecords": 1,
  "accessEvents": [
    {
      "eventId": "8f7b8d9f-0d85-4c42-9ee4-0bc7c4b1f4cc",
      "eventType": "ACCOUNT_VIEWED",
      "actorId": "user-123",
      "resourceType": "CLIENT_ACCOUNT",
      "resourceId": "account-456",
      "payload": {
        "screen": "account-summary"
      },
      "eventTimestamp": "2026-08-19T10:15:30Z",
      "createdAt": "2026-08-19T10:15:30.020Z",
      "previousHash": "0000000000000000000000000000000000000000000000000000000000000000",
      "currentHash": "f4f6a7d42c1b5f91e0f8c9a5c3a8d4f8f1f0d2e8d4c4e2a1a4c0f8a0f6e2d1b3",
      "hashAlgorithm": "SHA-256",
      "archived": false,
      "redacted": false
    }
  ]
}
```

### Validation Rules

- `page` must be at least 0.
- `size` must be between 1 and 100.
- Malformed timestamps return HTTP 400.
- The fixed resource type and event-type allowlist are always applied.
- Archived events are excluded by default.

### Notes

- Requires `AUDITOR` or `ADMIN`.
- This is an assignment-level traceability report, not a legal compliance certification.
- `totalRecords` currently represents the returned page content size for prototype simplicity.

## Error Response Contract

The global exception handler returns this shape for handled errors:

```json
{
  "timestamp": "2026-08-19T11:15:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Request parameter or body could not be parsed",
  "path": "/api/audit/events"
}
```

Typical statuses are:

| Status | Meaning |
| ---: | --- |
| `200` | Successful query, verification, export, compliance, or archival operation. |
| `201` | Event appended successfully. |
| `400` | Validation, parsing, or pagination error. |
| `401` | Missing or invalid authentication. |
| `403` | Authenticated user lacks the required role. |
| `404` | Requested event does not exist for redaction. |
| `500` | Unexpected server failure. |

## Append-Only Boundary

No update or delete API is exposed for audit events. Redaction and retention are controlled metadata operations only. Redaction does not rewrite the immutable original payload or hash fields, and retention does not physically delete records. This preserves the audit chain as the integrity source for verification.
