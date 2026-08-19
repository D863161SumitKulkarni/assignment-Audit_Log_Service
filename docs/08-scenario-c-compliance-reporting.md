# Scenario C: Compliance Reporting

## Original Ambiguous Requirement

> Regulators need to be able to audit access to client account data.

## 1. Why the Requirement Is Ambiguous

The requirement describes a business objective but does not define the data, actors, events, report contract, or regulatory controls needed to implement it consistently. In particular, it does not establish:

- What qualifies as client account data.
- Which actions count as access.
- Whether regulators are internal users, external authorities, or a specific role.
- Which time period and timezone a report must cover.
- Whether reports must be exported or only viewed through an API.
- Which payload fields may contain sensitive information and how they must be redacted.
- The required report format, delivery mechanism, and integrity evidence.
- The applicable retention period or legal hold requirements.
- The authorization, tenant isolation, and identity verification requirements for regulators.

Without these definitions, different implementations could all appear reasonable while producing incompatible or incomplete compliance evidence.

## 2. Clarifying Questions

Before treating this requirement as production-ready, the following questions should be answered:

1. What counts as client account data? Is it limited to account records, or does it include balances, profile data, statements, permissions, and related resources?
2. Which event types represent access? Should viewing, exporting, updating, permission changes, failed access, and administrative actions all be included?
3. Who are regulators? Are they internal compliance users, external regulators, auditors, or separate organizations with different permissions?
4. What time range is required? Should reports support an explicit start and end timestamp, a standard reporting period, and a defined timezone?
5. Is export required? If so, should the report be downloadable, streamed, signed, or independently verifiable?
6. Should sensitive payload data be redacted? Which fields are sensitive, and should redaction be role-specific or consistent for every report?
7. What format is required? Should the result be JSON, CSV, PDF, or a regulator-specific schema?
8. What retention period applies? Are archived records included, and must legal holds prevent archival or deletion?
9. What authorization is required before a regulator can access a report?
10. Must the report include chain hashes, verification status, or evidence of completeness?

## 3. Assumptions Made for This Prototype

For this assignment, the following assumptions define a deliberately narrow and testable scope:

- Client account data is represented by events where `resourceType` equals `CLIENT_ACCOUNT`.
- The report includes these access-related event types:
  - `ACCOUNT_VIEWED`
  - `ACCOUNT_EXPORTED`
  - `ACCOUNT_UPDATED`
  - `PERMISSION_GRANTED`
- `actorId`, `resourceId`, event type, and event timestamp provide the minimum access traceability fields.
- The report is returned through a backend API as structured JSON.
- The report uses the existing audit event query and response model rather than introducing a separate reporting store.
- Redacted response payloads are returned when an event has been redacted; the original payload remains the integrity source for hash verification.
- Archived records are excluded by default but can be included explicitly for historical compliance reporting because they remain part of the audit chain.
- A time range is supported by the underlying audit query contract, even though the exact regulator reporting period is not prescribed.
- Authentication, regulator authorization, and legal compliance certification are not defined by the assignment and are therefore not claimed by this prototype.

## 4. Normalized Requirement Statement

The service shall provide a compliance report endpoint that returns audit events for client account resources, where `resourceType` is `CLIENT_ACCOUNT` and `eventType` is one of `ACCOUNT_VIEWED`, `ACCOUNT_EXPORTED`, `ACCOUNT_UPDATED`, or `PERMISSION_GRANTED`. Each result shall expose the actor, resource, event type, event timestamp, applicable redacted payload, and integrity metadata through the standard audit response contract.

The report shall support a defined time range when requested, include archived records when the reporting policy requires historical completeness, and remain traceable to the append-only audit event chain. This normalized statement is an assignment-level interpretation, not a claim of regulatory certification.

## 5. Technical Design

### Data selection

The report is derived from the audit event table using these predicates:

- `resource_type = 'CLIENT_ACCOUNT'`
- `event_type IN ('ACCOUNT_VIEWED', 'ACCOUNT_EXPORTED', 'ACCOUNT_UPDATED', 'PERMISSION_GRANTED')`
- Optional inclusive event timestamp range
- Optional client account identifier through `resource_id`

Results are ordered by database `id` ascending to preserve audit chronology and stable chain order.

### Response model

The report returns a structured compliance response containing:

- Report generation timestamp.
- Client account identifier when a specific account is requested.
- Total matching records.
- Matching access events represented as `AuditEventResponse` records.

Each event response includes actor, resource, event, timestamp, archive and redaction flags, and hash-chain metadata. Redaction affects the response payload only; it does not rewrite the original payload or either hash commitment.

### Integrity

The report reads from the same append-only source used by normal audit queries. Chain verification recalculates hashes using the original stored payload, so redacted records remain verifiable. A separate chain verification endpoint can be used to identify the first inconsistency before relying on a report as evidence.

### API boundary

The compliance report is exposed as a read-only endpoint. No update or delete operation is introduced for reporting, and generating a report does not alter audit events.

## 6. Implemented Scope

The prototype implements the following Scenario C behavior:

- A compliance report contract based on `ComplianceReportResponse`.
- Filtering to `CLIENT_ACCOUNT` resources.
- Filtering to `ACCOUNT_VIEWED`, `ACCOUNT_EXPORTED`, `ACCOUNT_UPDATED`, and `PERMISSION_GRANTED` events.
- Identification of the actor, account resource, event type, and event timestamp.
- JSON response delivery through the backend API.
- Use of the existing audit event records and mapper.
- Redacted response payload handling without changing the original hash commitment.
- Inclusion of historical archived records when `includeArchived=true` is requested.
- Independent hash-chain verification using original payload data.

## 7. Out of Scope

- Regulatory certification, legal interpretation, or a claim that the implementation satisfies a specific law or framework.
- Regulator authentication, authorization, organization management, and tenant isolation.
- A regulator-specific CSV, PDF, signed document, or external delivery format.
- External compliance platforms, SIEM integration, or evidence notarization.
- A complete data classification catalogue for client account payloads.
- Automatic legal holds and regulation-specific retention policies.
- Real-time compliance alerts or anomaly detection.
- Reconstructing access events that were never recorded by the audit service.
- Changes to original payloads or hash values to produce a report.

## 8. Validation Approach

Validation should demonstrate both functional filtering and integrity behavior:

1. Insert representative events for each selected event type and at least one unrelated resource type or event type.
2. Request a report for `CLIENT_ACCOUNT` data and confirm that only the four selected event types are returned.
3. Confirm that the report identifies the expected actor, resource identifier, event type, and timestamp for each record.
4. Test a specific `resourceId` and an inclusive time range.
5. Test an empty result and confirm that the response remains valid with zero records.
6. Mark a matching event as archived and confirm the documented historical-reporting behavior.
7. Redact a matching event and confirm that the response payload is redacted while the original payload remains available to chain verification.
8. Run `GET /api/audit/verify` before using the report as integrity evidence.
9. Modify a stored original payload directly in a local database and confirm that verification reports `chainIntact: false` with `CURRENT_HASH_MISMATCH`.
10. Confirm that report generation itself does not create, update, or delete audit events.

## 9. Risks and Trade-offs

| Risk or trade-off | Impact | Mitigation |
| --- | --- | --- |
| Event-type allowlist may omit a legally relevant access action. | The report could be incomplete for a real regulator. | Confirm the access taxonomy before production use and make the allowlist configurable. |
| `CLIENT_ACCOUNT` is a convention rather than a governed data classification. | Related client data may be misclassified or missed. | Establish a resource taxonomy and ownership process. |
| JSON output may not satisfy a regulator's submission format. | Additional transformation or evidence packaging may be required. | Confirm the required format and export controls with stakeholders. |
| Redaction can reduce the detail available to a reviewer. | A report may prove access occurred without exposing enough context. | Define field-level redaction rules and regulator-specific disclosure policy. |
| Archived records remain reportable but ordinary queries may exclude them. | Users may misunderstand report completeness. | Document archive inclusion explicitly and expose the selected reporting scope. |
| Hash chaining detects changes but does not prove who caused them. | Integrity evidence is not the same as attribution or non-repudiation. | Add authentication, authorization, signatures, or external anchoring for production needs. |
| The report is derived at request time. | Large histories may affect latency and database load. | Use indexes, pagination or controlled export, and consider a reporting projection if scale requires it. |
| The assignment does not define regulator authorization. | Unauthorized access to the report could expose sensitive metadata. | Treat authentication and authorization as mandatory production controls before deployment. |

This interpretation is intentionally narrow, reviewable, and honest about the information missing from the original requirement.
