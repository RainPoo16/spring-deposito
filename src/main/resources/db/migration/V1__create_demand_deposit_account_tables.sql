CREATE TABLE demand_deposit_account (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    status VARCHAR(64) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE account_creation_idempotency (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    account_id UUID NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_account_creation_idempotency_customer_key UNIQUE (customer_id, idempotency_key)
);
