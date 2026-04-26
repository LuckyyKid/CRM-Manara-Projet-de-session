CREATE TABLE IF NOT EXISTS parent_subscription (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT NOT NULL UNIQUE,
    user_id BIGINT NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL,
    provider VARCHAR(32) NOT NULL,
    stripe_customer_id VARCHAR(255),
    stripe_subscription_id VARCHAR(255),
    stripe_checkout_session_id VARCHAR(255),
    stripe_price_id VARCHAR(255),
    stripe_additional_price_id VARCHAR(255),
    covered_children_count INTEGER NOT NULL DEFAULT 1,
    pending_covered_children_count INTEGER NOT NULL DEFAULT 1,
    first_child_monthly_amount_cents BIGINT NOT NULL DEFAULT 6000,
    additional_child_monthly_amount_cents BIGINT NOT NULL DEFAULT 4000,
    current_period_start TIMESTAMP,
    current_period_end TIMESTAMP,
    cancel_at_period_end BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS parent_subscription_child (
    id BIGSERIAL PRIMARY KEY,
    subscription_id BIGINT NOT NULL REFERENCES parent_subscription(id) ON DELETE CASCADE,
    enfant_id BIGINT NOT NULL REFERENCES enfant(id) ON DELETE CASCADE,
    created_at TIMESTAMP,
    CONSTRAINT uk_subscription_child UNIQUE (subscription_id, enfant_id)
);
