-- Add persistent balances with deterministic zero defaults for existing accounts
ALTER TABLE demand_deposit_account
    ADD COLUMN current_balance NUMERIC(19, 2) NOT NULL DEFAULT 0.00;

ALTER TABLE demand_deposit_account
    ADD COLUMN available_balance NUMERIC(19, 2) NOT NULL DEFAULT 0.00;

-- Stores durable transaction postings used for replay-safe processing and auditability
CREATE TABLE demand_deposit_account_transaction (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    transaction_type VARCHAR(16) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    transaction_code VARCHAR(64) NOT NULL,
    reference_id VARCHAR(128) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    posted_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_dda_transaction_account
        FOREIGN KEY (account_id) REFERENCES demand_deposit_account (id),
    CONSTRAINT uk_dda_transaction_account_reference UNIQUE (account_id, reference_id)
);

-- Stores idempotency identity to prevent duplicate postings across retries/replays
CREATE TABLE account_transaction_idempotency (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    account_id UUID NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    reference_id VARCHAR(128) NOT NULL,
    transaction_id UUID NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_account_transaction_idempotency_transaction
        FOREIGN KEY (transaction_id) REFERENCES demand_deposit_account_transaction (id),
    CONSTRAINT uk_account_transaction_idempotency_identity UNIQUE (customer_id, idempotency_key, reference_id)
);

-- Supports account transaction history access by account and posting time
CREATE INDEX idx_dda_transaction_account_posted_at
    ON demand_deposit_account_transaction (account_id, posted_at DESC);

-- Supports idempotency transaction-link lookups
CREATE INDEX idx_account_transaction_idempotency_transaction
    ON account_transaction_idempotency (transaction_id);
