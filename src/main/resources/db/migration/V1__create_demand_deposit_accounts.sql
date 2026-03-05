-- V1__create_demand_deposit_accounts.sql
-- Purpose: Create baseline table for customer main demand deposit account lifecycle.
-- Impact: Initial schema creation for Phase 1; no existing table rewrite.

CREATE TABLE demand_deposit_accounts (
    id UUID NOT NULL,
    customer_id UUID NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    status VARCHAR(64) NOT NULL,
    ledger_balance DECIMAL(19, 4) NOT NULL,
    available_balance DECIMAL(19, 4) NOT NULL,
    version BIGINT NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_demand_deposit_accounts PRIMARY KEY (id),
    CONSTRAINT uq_demand_deposit_accounts_customer_idempotency UNIQUE (customer_id, idempotency_key),
    -- Restrict lifecycle status to known Phase 1 states.
    CONSTRAINT chk_demand_deposit_accounts_status CHECK (status IN ('PENDING_VERIFICATION', 'ACTIVE')),
    -- Prevent negative starting balances.
    CONSTRAINT chk_demand_deposit_accounts_ledger_balance_non_negative CHECK (ledger_balance >= 0),
    CONSTRAINT chk_demand_deposit_accounts_available_balance_non_negative CHECK (available_balance >= 0)
);

CREATE INDEX idx_demand_deposit_accounts_customer_id
    ON demand_deposit_accounts (customer_id);

-- Supports queries by lifecycle status in future phases.
CREATE INDEX idx_demand_deposit_accounts_status
    ON demand_deposit_accounts (status);
