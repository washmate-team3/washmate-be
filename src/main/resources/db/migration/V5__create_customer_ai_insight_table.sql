-- Stores rule-based customer insights generated from booking_behavior_monthly.
-- The unique key lets the generator update the same insight on reruns instead
-- of creating duplicate rows for the same customer, garage, period, and model.
CREATE TABLE IF NOT EXISTS customer_ai_insight (
    insight_id SERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES app_user(user_id),
    garage_id INT NOT NULL REFERENCES garage(garage_id),
    period VARCHAR(7) NOT NULL,
    insight_type VARCHAR(50) NOT NULL,
    prediction_value JSONB NOT NULL,
    confidence_score DECIMAL(5,4),
    model_version VARCHAR(50) NOT NULL,
    generated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_customer_ai_insight
        UNIQUE (user_id, garage_id, period, insight_type, model_version),
    CONSTRAINT chk_customer_ai_insight_period
        CHECK (period ~ '^[0-9]{4}-(0[1-9]|1[0-2])$')
);

ALTER TABLE customer_ai_insight
    ADD COLUMN IF NOT EXISTS insight_id SERIAL;

ALTER TABLE customer_ai_insight
    ADD COLUMN IF NOT EXISTS user_id INT;

ALTER TABLE customer_ai_insight
    ADD COLUMN IF NOT EXISTS garage_id INT;

ALTER TABLE customer_ai_insight
    ADD COLUMN IF NOT EXISTS period VARCHAR(7);

ALTER TABLE customer_ai_insight
    ADD COLUMN IF NOT EXISTS insight_type VARCHAR(50);

ALTER TABLE customer_ai_insight
    ADD COLUMN IF NOT EXISTS prediction_value JSONB;

ALTER TABLE customer_ai_insight
    ADD COLUMN IF NOT EXISTS confidence_score DECIMAL(5,4);

ALTER TABLE customer_ai_insight
    ADD COLUMN IF NOT EXISTS model_version VARCHAR(50);

ALTER TABLE customer_ai_insight
    ADD COLUMN IF NOT EXISTS generated_at TIMESTAMPTZ DEFAULT NOW();

CREATE INDEX IF NOT EXISTS idx_customer_ai_insight_garage_period
    ON customer_ai_insight(garage_id, period, generated_at DESC);

CREATE INDEX IF NOT EXISTS idx_customer_ai_insight_user_generated_at
    ON customer_ai_insight(user_id, generated_at DESC);
