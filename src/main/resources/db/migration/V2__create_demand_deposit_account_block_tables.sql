-- Stores account-level block lifecycle records for overlap and status checks
CREATE TABLE demand_deposit_account_block (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL,
    block_code VARCHAR(16) NOT NULL,
    requested_by VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    effective_date DATE NOT NULL,
    expiry_date DATE NOT NULL,
    remark VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_demand_deposit_account_block_account
        FOREIGN KEY (account_id) REFERENCES demand_deposit_account (id)
);

-- Supports overlap checks for active or pending blocks by account and code
CREATE INDEX idx_dda_block_overlap_lookup
    ON demand_deposit_account_block (account_id, block_code, status, effective_date, expiry_date);
