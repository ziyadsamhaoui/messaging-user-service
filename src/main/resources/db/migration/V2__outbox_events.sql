-- Sprint 6: transactional outbox for domain events (see docs/EVENTS.md and docs/adr/ADR-007).
-- Rows are inserted in the SAME transaction as the domain write; a polling relay
-- publishes them to Kafka and stamps published_at afterwards.
CREATE TABLE outbox_events (
    id             UUID PRIMARY KEY,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id   VARCHAR(100) NOT NULL,
    event_type     VARCHAR(100) NOT NULL,
    event_version  INT NOT NULL,
    payload        JSONB NOT NULL,
    correlation_id VARCHAR(100),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at   TIMESTAMPTZ NULL
);

-- Relay scan index: only unpublished rows, oldest first.
CREATE INDEX idx_outbox_unpublished ON outbox_events (created_at) WHERE published_at IS NULL;

-- Sprint 6 §3.4: consumer-side idempotency ledger for events whose side effects are
-- not naturally idempotent (e.g. CREDENTIAL_REGISTERED replayed after a username
-- conflict). Checked before applying, written in the same transaction as the apply.
CREATE TABLE processed_event_ids (
    event_id    UUID PRIMARY KEY,
    event_type  VARCHAR(100) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
