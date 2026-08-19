# Task Decomposition

## Purpose

This document decomposes the tamper-evident audit log service into an ordered implementation plan. It defines dependencies, validation gates, AI usage traceability, and human review responsibilities so that the work can be executed and defended as an AI-assisted engineering assignment.

## Delivery Sequence and Dependencies

| Phase | Workstream | Depends on | Exit condition |
| --- | --- | --- | --- |
| 1 | Repository setup and documentation scaffold | None | Repository structure and traceability files are ready |
| 2 | Requirement analysis and assumptions | Phase 1 | Requirements, assumptions, and ambiguity decisions are recorded |
| 3 | Architecture and API design | Phase 2 | Design and API contracts are reviewable |
| 4 | Spring Boot backend generation | Phase 3 | Application builds and starts with the selected stack |
| 5 | Database schema | Phase 4 | PostgreSQL schema and migrations are executable |
| 6 | Scenario A implementation | Phases 3-5 | Append, query, chain verification, and tamper detection work |
| 7 | Scenario B implementation | Phase 6 | Retention, redaction, and verifiable export work |
| 8 | Scenario C implementation | Phases 2, 6, and 7 | Compliance report works within documented boundaries |
| 9 | Testing | Each implemented feature | Automated and database-level validation passes |
| 10 | Documentation and final engineering summary | Phases 1-9 | Repository explains decisions, evidence, limitations, and AI use |
| 11 | Live defense preparation | Phases 1-10 | The candidate can demonstrate and justify the system |

Every phase ends with a human review of the output artifacts. AI-generated suggestions are treated as proposals and are not accepted until checked against the requirements, code, tests, and observed behavior.

## 1. Repository Setup and Documentation Scaffold

| Task | Goal | Inputs | Output artifacts | Acceptance criteria | Suggested commit message | AI usage note |
| --- | --- | --- | --- | --- | --- | --- |
| 1.1 Inventory the repository | Establish the starting point and identify existing files, conventions, and gaps. | Repository tree and assignment brief | Initial repository inventory and gap list | Relevant directories, existing documents, and empty or incomplete areas are identified. | `docs: inventory assignment repository` | AI may summarize the tree; the candidate confirms the inventory by inspecting the repository. |
| 1.2 Define documentation structure | Make the engineering process easy to review. | Assignment phases and expected deliverables | Numbered documents for requirements, design, implementation, testing, risks, and defense | Each required concern has an obvious document location and naming convention. | `docs: scaffold engineering documentation` | AI may suggest document organization; the candidate selects the structure and owns the final scope. |
| 1.3 Add AI traceability structure | Record prompts, assistance received, human decisions, and validation honestly. | AI-assisted assignment expectations | `ai-usage/AI_USAGE_LOG.md` and prompt history structure | The traceability location and recording format are defined before implementation begins. | `docs: add AI usage traceability scaffold` | AI can help draft the template; all entries must be completed by the candidate from actual interactions. |

**Phase 1 validation:** Confirm that a reviewer can locate the requirements, design, test strategy, AI usage log, and final notes without opening source code.

## 2. Requirement Analysis and Assumptions

| Task | Goal | Inputs | Output artifacts | Acceptance criteria | Suggested commit message | AI usage note |
| --- | --- | --- | --- | --- | --- | --- |
| 2.1 Extract functional requirements | Convert the brief into testable behavior. | Assignment statement | `docs/01-requirement-analysis.md` functional requirements | Append-only events, required fields, filters, pagination, hashes, verification, and scenarios B/C are listed. | `docs: define audit service requirements` | AI may identify implicit requirements; the candidate checks every item against the assignment brief. |
| 2.2 Record engineering assumptions | Make unresolved decisions visible rather than hiding them in implementation. | Technology choices and constraints | Assumptions, non-functional requirements, and out-of-scope list | Server timestamps, SHA-256, soft archival, structured redaction, backend-only scope, and OpenAPI are documented. | `docs: record audit service assumptions` | AI may propose assumptions; the candidate accepts, rejects, or qualifies each one. |
| 2.3 Resolve Scenario C ambiguity | Define a defensible minimum interpretation of compliance reporting. | Requirement: regulators audit client account access | Explicit ambiguity handling and report boundary | The interpretation identifies actor, resource, event, and time, while naming unknown regulation and authorization details. | `docs: clarify compliance reporting scope` | AI may present interpretations; the candidate selects one and records why it is appropriate for the assignment. |
| 2.4 Define acceptance criteria | Establish the evidence needed to claim completion. | Functional and non-functional requirements | Requirement-level acceptance checklist | Each major requirement maps to an observable API, database, test, or documentation result. | `docs: add requirement acceptance criteria` | AI may turn prose into checks; the candidate verifies that each criterion is measurable. |

**Phase 2 validation:** Review the requirements document against the assignment line by line and confirm that no design choice silently changes the requested behavior.

## 3. Architecture and API Design

| Task | Goal | Inputs | Output artifacts | Acceptance criteria | Suggested commit message | AI usage note |
| --- | --- | --- | --- | --- | --- | --- |
| 3.1 Define service boundaries | Separate controllers, application services, persistence, hashing, redaction, export, and reporting responsibilities. | Phase 2 requirements | `docs/03-architecture-overview.md` | Responsibilities and data flow are clear, with no update/delete path for audit events. | `docs: define audit service architecture` | AI may suggest a layered structure; the candidate checks that it fits the assignment and selected stack. |
| 3.2 Design the append contract | Define safe event ingestion and server-owned fields. | Required event fields and hash decisions | `docs/04-api-design.md` append endpoint contract | Client cannot supply or override the authoritative timestamp or chain hashes. | `docs: design audit append API` | AI may draft request and response fields; the candidate removes unsafe or unnecessary inputs. |
| 3.3 Design query and verification contracts | Make filtering, pagination, and first-failure reporting unambiguous. | Query and verification requirements | Query parameters, pagination model, and `GET /audit/verify` response contract | All required filters are represented and verification identifies the first inconsistency. | `docs: design query and verification APIs` | AI may suggest pagination and error shapes; the candidate checks consistency and testability. |
| 3.4 Design Scenario B and C contracts | Define archival, redaction, export, and compliance report behavior. | Scenario B/C interpretation | API contracts and scope notes | Redaction does not rewrite the committed original; export remains verifiable; compliance boundaries are stated. | `docs: design retention export and compliance APIs` | AI may help compare contract options; the candidate decides the public behavior and documents tradeoffs. |
| 3.5 Specify canonical hash input | Ensure every implementation and test uses the same commitment rule. | Required record fields and SHA-256 decision | `docs/05-hash-chain-designing.md` | Field order, encoding, previous-hash handling, and first-record behavior are defined. | `docs: specify audit hash chain` | AI may propose canonicalization approaches; the candidate validates determinism and edge cases. |

**Phase 3 validation:** Conduct a human API review using Swagger/OpenAPI planning and confirm that every acceptance criterion can be exercised without undocumented behavior.

## 4. Spring Boot Backend Generation

| Task | Goal | Inputs | Output artifacts | Acceptance criteria | Suggested commit message | AI usage note |
| --- | --- | --- | --- | --- | --- | --- |
| 4.1 Create the Maven project | Establish the Java 21 and Spring Boot 3 build. | Architecture and selected technologies | `pom.xml`, application entry point, standard source layout | The project compiles with Java 21 and dependencies are minimal and justified. | `build: initialize Spring Boot audit service` | AI may generate Maven configuration; the candidate verifies versions, dependencies, and licensing suitability. |
| 4.2 Configure application environments | Separate local, test, and database configuration. | PostgreSQL choice and test strategy | Configuration files and environment documentation | The application can start with documented configuration and does not commit secrets. | `build: configure audit service environments` | AI may suggest profiles; the candidate checks secret handling and reproducibility. |
| 4.3 Add API documentation support | Make contracts available for review. | API design | OpenAPI configuration and endpoint metadata | Swagger/OpenAPI is accessible when the application runs and reflects the intended contracts. | `build: add OpenAPI documentation` | AI may draft annotations or descriptions; the candidate verifies that documentation matches behavior. |

**Phase 4 validation:** Run the Maven build and start the application with a local profile. Record the exact commands and results in the AI usage or execution notes where relevant.

## 5. Database Schema

| Task | Goal | Inputs | Output artifacts | Acceptance criteria | Suggested commit message | AI usage note |
| --- | --- | --- | --- | --- | --- | --- |
| 5.1 Model audit records | Persist required event data and integrity metadata. | Hash-chain design and event contract | PostgreSQL table definition and migration | The schema stores all required fields, hashes, server timestamp, and archival state without update/delete API assumptions. | `db: create audit event schema` | AI may suggest column types and constraints; the candidate reviews nullability, precision, and data sensitivity. |
| 5.2 Add query indexes and constraints | Support required filters and protect invalid data. | Query contract and expected access patterns | Indexes, uniqueness or ordering constraints, and validation rules | Actor, resource, event type, and timestamp queries have appropriate indexes; invalid records are rejected. | `db: add audit query indexes and constraints` | AI may recommend indexes; the candidate confirms them against actual query patterns and chain ordering. |
| 5.3 Define archival persistence | Retain records while excluding them from ordinary active views where appropriate. | Soft archival decision | Archival columns and retention state rules | Archival does not remove a record or alter its committed content. | `db: support soft archival retention` | AI may suggest state modeling; the candidate ensures the model preserves verification and export requirements. |

**Phase 5 validation:** Apply the schema to a clean PostgreSQL database, inspect constraints and indexes, and confirm that a deliberately altered hash or link can remain detectable by verification rather than being silently normalized.

## 6. Scenario A Implementation

| Task | Goal | Inputs | Output artifacts | Acceptance criteria | Suggested commit message | AI usage note |
| --- | --- | --- | --- | --- | --- | --- |
| 6.1 Implement append-only write API | Accept valid events and persist them in chain order. | API contract, schema, server timestamp rule | Append controller, service, validation, and persistence behavior | Valid events are stored; timestamp and hashes are server-controlled; update and delete operations are absent. | `feat: implement append-only audit writes` | AI may assist with Spring patterns and validation; the candidate reviews transaction boundaries and rejects unsafe generated behavior. |
| 6.2 Implement query API | Retrieve audit events with required filters and pagination. | Query contract and indexes | Query controller, service, repository queries, and response mapping | Every required filter works independently and in combination, with stable pagination and redacted response payloads. | `feat: implement paginated audit queries` | AI may draft repository predicates; the candidate checks empty filters, boundaries, ordering, and payload exposure. |
| 6.3 Implement hash-chain service | Calculate and persist deterministic SHA-256 commitments. | Canonical hash specification | Hashing service and chain-link persistence | The first event uses the defined initial link; later events reference the immediately preceding hash; repeated inputs produce the same hash. | `feat: add SHA-256 audit hash chain` | AI may help with hashing APIs; the candidate validates canonicalization independently with known values. |
| 6.4 Implement verification endpoint | Verify links and record hashes across the chain. | Hash-chain service and API contract | `GET /audit/verify` implementation and response model | A valid chain passes; the first broken link or mismatched hash is reported with useful record context. | `feat: add audit chain verification` | AI may suggest traversal logic; the candidate reviews ordering, first-failure behavior, and error handling. |
| 6.5 Validate tamper detection | Demonstrate that direct database changes are observable. | Running service, populated database, verification endpoint | Scenario A validation notes and reproducible tamper procedure | A changed payload, hash, or previous link causes verification to fail at the first affected record. | `test: validate audit tamper detection` | AI may suggest tamper cases; the candidate performs the database mutation and records observed results honestly. |

**Phase 6 validation:** Execute the happy path, invalid input cases, pagination queries, a valid chain verification, and direct SQL tampering followed by verification.

## 7. Scenario B Implementation

| Task | Goal | Inputs | Output artifacts | Acceptance criteria | Suggested commit message | AI usage note |
| --- | --- | --- | --- | --- | --- | --- |
| 7.1 Implement retention archival | Apply retention without physical deletion. | Retention assumptions and schema | Retention service or operation and archival behavior | Eligible records become soft-archived, remain in the chain, and remain available for verification. | `feat: implement soft archival retention` | AI may suggest scheduling or service patterns; the candidate chooses a bounded assignment-appropriate approach and verifies its effect. |
| 7.2 Implement structured redaction | Protect sensitive response fields while preserving integrity evidence. | Payload shape and redaction rules | Redaction policy, response mapper, and tests | Redaction is field-aware and deterministic; stored original content and hash commitment are not rewritten by a read operation. | `feat: add structured audit payload redaction` | AI may propose JSON traversal approaches; the candidate checks nested fields, missing fields, and accidental data leakage. |
| 7.3 Implement verifiable bulk export | Export selected records with enough metadata to verify their provenance. | Query contract, redaction behavior, hash chain | Export endpoint/service and export format documentation | Export honors filters and redaction, includes chain metadata, and clearly identifies whether the export is complete or filtered. | `feat: add verifiable audit export` | AI may draft serialization and streaming options; the candidate verifies ordering, boundaries, and redaction consistency. |

**Phase 7 validation:** Archive eligible records, query redacted data, export a filtered range, and verify that exported hash metadata agrees with the stored chain and documented scope.

## 8. Scenario C Implementation

| Task | Goal | Inputs | Output artifacts | Acceptance criteria | Suggested commit message | AI usage note |
| --- | --- | --- | --- | --- | --- | --- |
| 8.1 Confirm ambiguity boundary | Prevent unsupported compliance claims. | Scenario C wording and Phase 2 interpretation | Compliance scope note and unresolved questions | The implementation states what “audit access” means for this assignment and what remains unspecified. | `docs: define compliance reporting boundary` | AI may identify missing compliance details; the candidate decides which assumptions are safe to implement and which remain open. |
| 8.2 Implement compliance report endpoint | Provide traceability for client account access. | Audit query capability and scope note | Report endpoint/service and response documentation | A report identifies actor, client account resource, event type, and time range, using the same source-of-truth events as the audit API. | `feat: add client access compliance report` | AI may propose aggregation fields; the candidate ensures the report does not imply legal certification or invent unavailable data. |
| 8.3 Enforce scope boundaries | Keep authorization and regulatory policy claims explicit. | Out-of-scope decisions | Boundary documentation, validation, and appropriate response behavior | Unsupported regulator identity, permissions, certification, and report formats are not presented as implemented features. | `docs: document compliance scope limitations` | AI may help phrase limitations; the candidate checks that documentation matches actual controls. |

**Phase 8 validation:** Generate a report for known client-account access events, test an empty range, and verify that the endpoint remains consistent with the documented ambiguity handling.

## 9. Testing

| Task | Goal | Inputs | Output artifacts | Acceptance criteria | Suggested commit message | AI usage note |
| --- | --- | --- | --- | --- | --- | --- |
| 9.1 Write unit tests | Verify hashing, canonicalization, validation, redaction, and report mapping in isolation. | Service and utility behavior | Focused unit test suite | Deterministic hashes, first-record behavior, invalid input, nested redaction, and report mapping are covered. | `test: add audit service unit coverage` | AI may generate test cases; the candidate reviews assertions and adds edge cases that reflect the requirements. |
| 9.2 Write service tests | Verify transactions, chain sequencing, archival, querying, and export orchestration. | Application services and repositories | Service-level tests with controlled persistence | Services enforce append-only behavior, preserve chain order, apply redaction, and retain archival records. | `test: cover audit service workflows` | AI may suggest fixtures and mocks; the candidate checks that tests validate behavior rather than implementation details. |
| 9.3 Write controller integration tests | Validate API contracts across HTTP, validation, persistence, and serialization. | Running Spring context and database test setup | Controller integration test suite | Append, query, verification, retention, export, and compliance endpoints return documented statuses and bodies. | `test: add audit API integration coverage` | AI may scaffold MockMvc or HTTP tests; the candidate confirms real contract behavior and negative cases. |
| 9.4 Perform tamper SQL validation | Prove that the service detects database-level alteration. | PostgreSQL test data and verification endpoint | Repeatable SQL tamper test notes | Payload, current hash, and previous hash modifications each produce an actionable first inconsistency. | `test: exercise SQL tamper scenarios` | AI may enumerate mutation cases; the candidate executes them against a controlled database and records actual output. |
| 9.5 Run regression and quality checks | Ensure the complete assignment remains buildable and reviewable. | All source, tests, and documentation | Test report, build result, and unresolved issue list | Build, tests, API checks, and documentation checks pass or known failures are explicitly recorded. | `test: run full audit service validation` | AI may help interpret failures; the candidate determines whether failures are fixed, accepted, or documented. |

**Phase 9 validation:** The test result is the primary completion gate. No feature is considered complete based only on generated code or a passing compilation step.

## 10. Documentation and Final Engineering Summary

| Task | Goal | Inputs | Output artifacts | Acceptance criteria | Suggested commit message | AI usage note |
| --- | --- | --- | --- | --- | --- | --- |
| 10.1 Complete implementation documentation | Explain how the service satisfies the requirements. | Final code, tests, and observed behavior | Architecture, API, hash, scenario, and testing documents | Documentation describes actual behavior, not planned behavior, and links decisions to validation evidence. | `docs: complete audit service engineering documentation` | AI may help edit for clarity; the candidate verifies every technical claim against the implementation. |
| 10.2 Record risks and tradeoffs | Make limitations and production gaps visible. | Design decisions and test findings | Risks, tradeoffs, and limitations document | Soft archival, SHA-256 scope, compliance ambiguity, authorization gaps, and operational risks are stated. | `docs: record audit service risks and tradeoffs` | AI may identify common risks; the candidate keeps only risks relevant to this implementation. |
| 10.3 Update AI usage history | Provide honest process traceability. | Actual prompts, suggestions, edits, and validation results | Completed `ai-usage/AI_USAGE_LOG.md` and `docs/Prompt_History.md` | The record distinguishes AI suggestions from human decisions and does not claim work or validation that did not occur. | `docs: finalize AI-assisted execution record` | The candidate owns the factual record; AI may help organize entries but must not fabricate them. |
| 10.4 Write final engineering summary | Give reviewers a concise view of outcome and remaining limits. | Requirements, implementation, tests, and risks | Final summary with feature status and evidence | Summary states what was delivered, how it was validated, and what remains out of scope. | `docs: add final engineering summary` | AI may help condense the summary; the candidate verifies its accuracy and tone. |

**Phase 10 validation:** A reviewer unfamiliar with the implementation can trace each major requirement to a design decision, an API or schema artifact, and a validation result.

## 11. Live Defense Preparation

| Task | Goal | Inputs | Output artifacts | Acceptance criteria | Suggested commit message | AI usage note |
| --- | --- | --- | --- | --- | --- | --- |
| 11.1 Prepare the system walkthrough | Demonstrate the core flow from append to verification. | Running application, OpenAPI page, sample data | Short deterministic demonstration script | The walkthrough shows a valid append, filtered query, chain verification, tampering, and first-inconsistency reporting. | `docs: prepare live audit service walkthrough` | AI may help sequence the demo; the candidate rehearses it and confirms every result on the actual system. |
| 11.2 Prepare Scenario B demonstration | Explain retention, redaction, and export tradeoffs. | Scenario B implementation and validation | Scenario B demo checklist | The candidate can show soft archival, explain original hash preservation, and verify export scope and metadata. | `docs: prepare Scenario B defense notes` | AI may suggest likely reviewer questions; the candidate answers from observed behavior and documented limits. |
| 11.3 Prepare Scenario C explanation | Defend the compliance interpretation without overstating it. | Ambiguity notes and report endpoint | Scenario C explanation and open-question list | The candidate clearly distinguishes implemented traceability from regulation-specific authorization or certification. | `docs: prepare Scenario C defense notes` | AI may role-play questions; the candidate validates answers against the actual scope and evidence. |
| 11.4 Prepare AI-assisted engineering explanation | Explain how AI was used responsibly. | AI usage log, prompt history, commits, test results | Live defense notes | The candidate can identify AI contributions, human review points, rejected suggestions, and independent validation. | `docs: prepare AI usage defense notes` | AI may help organize talking points; the candidate must describe only genuine usage and decisions. |
| 11.5 Rehearse failure and tradeoff questions | Demonstrate engineering judgment under review. | Risks, limitations, and test evidence | Questions-and-answers checklist | The candidate can explain concurrency, chain recovery, redaction, retention, authorization, and production hardening limitations. | `docs: prepare engineering defense Q&A` | AI may generate questions; the candidate tests answers against repository evidence and refines them. |

**Phase 11 validation:** Complete a timed rehearsal on a clean environment and confirm that the demo works from documented setup instructions without relying on hidden local state.

## Cross-Phase Human Review Gates

| Gate | Review question | Evidence |
| --- | --- | --- |
| Requirements gate | Does the plan cover every assignment requirement and label ambiguity honestly? | Requirement analysis and acceptance criteria |
| Design gate | Can each public behavior be explained before implementation? | Architecture, API, and hash-chain documents |
| Integrity gate | Can a reviewer observe and reproduce first-failure tamper detection? | Verification response and SQL tamper validation |
| Scenario gate | Do retention, redaction, export, and compliance reporting preserve stated boundaries? | Scenario validation notes and tests |
| Quality gate | Are tests checking behavior at unit, service, HTTP, and database levels? | Test suite and test report |
| Traceability gate | Can the candidate distinguish AI assistance from human ownership? | AI usage log, prompt history, commits, and review notes |
| Submission gate | Does the repository describe actual delivered behavior and known limitations? | Final engineering summary and attestation |

## AI Usage and Human Review Protocol

For each AI-assisted task, record:

1. The question or prompt given to the AI.
2. The suggestion, draft, or analysis received.
3. The human decision: accepted, modified, or rejected.
4. The validation used, such as a test, build, API call, SQL inspection, or documentation review.
5. Any limitation or uncertainty that remains.

The candidate remains responsible for requirements interpretation, source changes, security and privacy decisions, test adequacy, and the accuracy of all repository documentation. AI assistance does not replace human review or execution evidence.
