-- V2__create_demand_deposit_account_lifecycle_events.sql
-- Purpose: Persist lifecycle events for FR-AC-4 outbox hook in the same transaction as account state changes.
-- Impact: Adds immutable event table for account created/activated events.

CREATE TABLE demand_deposit_account_lifecycle_events (
    id UUID NOT NULL,
    account_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    account_status VARCHAR(64) NOT NULL,
    occurred_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_demand_deposit_account_lifecycle_events PRIMARY KEY (id),
    CONSTRAINT fk_dda_lifecycle_event_account FOREIGN KEY (account_id)
        REFERENCES demand_deposit_accounts (id),
    CONSTRAINT chk_dda_lifecycle_event_type CHECK (event_type IN ('ACCOUNT_CREATED', 'ACCOUNT_ACTIVATED')),
    CONSTRAINT chk_dda_lifecycle_event_status CHECK (account_status IN ('PENDING_VERIFICATION', 'ACTIVE'))
);

CREATE INDEX idx_dda_lifecycle_events_account_id
    ON demand_deposit_account_lifecycle_events (account_id);

CREATE INDEX idx_dda_lifecycle_events_customer_id
    ON demand_deposit_account_lifecycle_events (customer_id);

CREATE INDEX idx_dda_lifecycle_events_occurred_at
    ON demand_deposit_account_lifecycle_events (occurred_at);
