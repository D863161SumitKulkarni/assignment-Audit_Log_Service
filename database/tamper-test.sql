-- Tamper detection test flow:
-- 1. Inspect the first few audit events.
-- 2. Change payload_original directly without changing current_hash.
-- 3. Inspect the modified event.
-- 4. Call GET /api/audit/verify from the running service.
-- 5. Restore the original payload after local testing if required.

-- Inspect the first few audit event records before tampering.
SELECT id,
       event_id,
       event_type,
       payload_original,
       previous_hash,
       current_hash
FROM audit.event
ORDER BY id ASC
LIMIT 5;

-- Directly tamper with the first existing record.
-- current_hash is intentionally left unchanged.
BEGIN;

WITH target AS (
    SELECT id
    FROM audit.event
    ORDER BY id ASC
    LIMIT 1
)
UPDATE audit.event AS event_record
SET payload_original = jsonb_set(
        event_record.payload_original,
        '{_tamper_test}',
        '"modified-by-tamper-test"'::jsonb,
        true
    )
FROM target
WHERE event_record.id = target.id;

COMMIT;

-- Inspect the modified record and confirm current_hash was not changed.
SELECT id,
       event_id,
       payload_original,
       previous_hash,
       current_hash
FROM audit.event
WHERE id = (
    SELECT MIN(id)
    FROM audit.event
);

-- Expected result from the application:
-- GET /api/audit/verify returns HTTP 200 with:
-- chainIntact: false
-- violationType: CURRENT_HASH_MISMATCH

-- Rollback note for local testing only:
-- The tamper update above is committed so the application can observe it.
-- Restore the database from a local backup, or remove only the test field with:
-- UPDATE audit.event
-- SET payload_original = payload_original - '_tamper_test'
-- WHERE id = (SELECT MIN(id) FROM audit.event);
-- Do not run the restoration statement if the original payload already contained
-- an intentional _tamper_test field.
