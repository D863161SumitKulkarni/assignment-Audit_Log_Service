CREATE SCHEMA IF NOT EXISTS audit;

CREATE TABLE audit.event (
	id BIGSERIAL PRIMARY KEY,
	event_id UUID UNIQUE NOT NULL,
	event_type VARCHAR(100) NOT NULL,
	actor_id VARCHAR(150) NOT NULL,
	resource_type VARCHAR(100) NOT NULL,
	resource_id VARCHAR(150) NOT NULL,
	payload_original JSONB NOT NULL,
	payload_redacted JSONB,
	event_timestamp TIMESTAMPTZ NOT NULL,
	created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	previous_hash VARCHAR(64) NOT NULL,
	current_hash VARCHAR(64) NOT NULL,
	hash_algorithm VARCHAR(50) NOT NULL DEFAULT 'SHA-256',
	archived BOOLEAN NOT NULL DEFAULT FALSE,
	archived_at TIMESTAMPTZ,
	redacted BOOLEAN NOT NULL DEFAULT FALSE,
	redacted_at TIMESTAMPTZ,
	redaction_reason TEXT
);

COMMENT ON TABLE audit.event IS
	'Append-only audit events with a tamper-evident hash chain.';

COMMENT ON COLUMN audit.event.previous_hash IS
	'SHA-256 hash of the preceding event in the audit chain.';

COMMENT ON COLUMN audit.event.current_hash IS
	'SHA-256 hash of this event and its previous hash commitment.';

COMMENT ON COLUMN audit.event.archived IS
	'Soft-archive marker; audit rows remain available for chain verification.';

CREATE INDEX idx_audit_event_actor_id
	ON audit.event (actor_id);

CREATE INDEX idx_audit_event_resource
	ON audit.event (resource_type, resource_id);

CREATE INDEX idx_audit_event_event_type
	ON audit.event (event_type);

CREATE INDEX idx_audit_event_event_timestamp
	ON audit.event (event_timestamp);

CREATE INDEX idx_audit_event_current_hash
	ON audit.event (current_hash);

CREATE INDEX idx_audit_event_previous_hash
	ON audit.event (previous_hash);

CREATE INDEX idx_audit_event_archived
	ON audit.event (archived);
