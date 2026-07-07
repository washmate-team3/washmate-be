CREATE TABLE IF NOT EXISTS insight_analysis_run (
    analysis_run_id SERIAL PRIMARY KEY,
    garage_id INT REFERENCES garage(garage_id),
    requested_by INT NOT NULL REFERENCES app_user(user_id),
    period_from DATE NOT NULL,
    period_to DATE NOT NULL,
    ai_model VARCHAR(100) NOT NULL,
    prompt_version VARCHAR(50),
    raw_response TEXT,
    total_returned INT NOT NULL DEFAULT 0,
    total_kept INT NOT NULL DEFAULT 0,
    total_rejected INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_insight_analysis_run_period CHECK (period_from <= period_to)
);

CREATE INDEX IF NOT EXISTS idx_insight_analysis_run_garage_created
    ON insight_analysis_run(garage_id, created_at);
