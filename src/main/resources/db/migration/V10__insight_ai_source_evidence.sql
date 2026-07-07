ALTER TABLE insight_ai_enrichment
    ADD COLUMN IF NOT EXISTS source VARCHAR(20) NOT NULL DEFAULT 'RULE_BASED',
    ADD COLUMN IF NOT EXISTS evidence_json JSONB,
    ADD COLUMN IF NOT EXISTS verified BOOLEAN NOT NULL DEFAULT TRUE;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_insight_ai_enrichment_source'
    ) THEN
        ALTER TABLE insight_ai_enrichment
            ADD CONSTRAINT chk_insight_ai_enrichment_source
            CHECK (source IN ('RULE_BASED', 'AI_DETECTED'));
    END IF;
END
$$;

CREATE INDEX IF NOT EXISTS idx_insight_ai_enrichment_source_verified
    ON insight_ai_enrichment(source, verified);
