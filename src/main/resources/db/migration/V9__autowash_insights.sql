CREATE TABLE IF NOT EXISTS insight_rule_config (
    rule_config_id SERIAL PRIMARY KEY,
    rule_code VARCHAR(100) NOT NULL UNIQUE,
    rule_name VARCHAR(255) NOT NULL,
    type VARCHAR(30) NOT NULL CHECK (type IN ('REVENUE','ORDER','SERVICE','CUSTOMER','LOYALTY')),
    threshold_value DECIMAL(10,2) NOT NULL,
    comparison_operator VARCHAR(40) NOT NULL CHECK (comparison_operator IN ('GREATER_THAN','LESS_THAN','GREATER_THAN_OR_EQUAL','LESS_THAN_OR_EQUAL','EQUAL')),
    severity VARCHAR(30) NOT NULL CHECK (severity IN ('CRITICAL','WARNING','OPPORTUNITY','POSITIVE')),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS business_insight (
    insight_id SERIAL PRIMARY KEY,
    rule_code VARCHAR(100) NOT NULL,
    type VARCHAR(30) NOT NULL CHECK (type IN ('REVENUE','ORDER','SERVICE','CUSTOMER','LOYALTY')),
    severity VARCHAR(30) NOT NULL CHECK (severity IN ('CRITICAL','WARNING','OPPORTUNITY','POSITIVE')),
    title VARCHAR(255) NOT NULL,
    summary TEXT NOT NULL,
    evidence TEXT NOT NULL,
    meaning TEXT NOT NULL,
    recommendation TEXT NOT NULL,
    related_metric VARCHAR(100),
    from_date DATE NOT NULL,
    to_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'NEW' CHECK (status IN ('NEW','READ','IN_PROGRESS','RESOLVED','DISMISSED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    CONSTRAINT fk_business_insight_rule FOREIGN KEY (rule_code) REFERENCES insight_rule_config(rule_code),
    CONSTRAINT uq_business_insight_rule_period UNIQUE (rule_code, from_date, to_date),
    CONSTRAINT chk_business_insight_period CHECK (from_date <= to_date)
);

CREATE INDEX IF NOT EXISTS idx_business_insight_period_type_status
    ON business_insight(from_date, to_date, type, status);

INSERT INTO insight_rule_config(rule_code, rule_name, type, threshold_value, comparison_operator, severity, is_active, description)
VALUES
    ('REVENUE_DROP', 'Doanh thu giảm mạnh', 'REVENUE', 15, 'LESS_THAN', 'WARNING', TRUE,
     'Doanh thu kỳ hiện tại giảm hơn 15% so với kỳ trước.'),
    ('REVENUE_GROWTH', 'Doanh thu tăng mạnh', 'REVENUE', 15, 'GREATER_THAN', 'POSITIVE', TRUE,
     'Doanh thu kỳ hiện tại tăng hơn 15% so với kỳ trước.'),
    ('WEEKEND_REVENUE_HIGH', 'Doanh thu cuối tuần cao', 'REVENUE', 30, 'GREATER_THAN', 'OPPORTUNITY', TRUE,
     'Doanh thu cuối tuần cao hơn ngày thường trên 30%.'),
    ('ORDER_CANCEL_RATE_HIGH', 'Tỷ lệ đơn hủy cao', 'ORDER', 10, 'GREATER_THAN', 'WARNING', TRUE,
     'Tỷ lệ đơn hủy/từ chối/no-show cao hơn 10%.'),
    ('PEAK_HOUR_ORDERS', 'Khung giờ cao điểm', 'ORDER', 30, 'GREATER_THAN', 'OPPORTUNITY', TRUE,
     'Một khung giờ chiếm hơn 30% tổng số đơn.'),
    ('ORDER_VALUE_LOW', 'Giá trị đơn trung bình thấp', 'ORDER', 15, 'GREATER_THAN', 'OPPORTUNITY', TRUE,
     'Số đơn tăng nhưng doanh thu không tăng tương ứng.'),
    ('DOMINANT_SERVICE_REVENUE', 'Dịch vụ đóng góp doanh thu lớn', 'SERVICE', 40, 'GREATER_THAN', 'OPPORTUNITY', TRUE,
     'Một dịch vụ chiếm hơn 40% tổng doanh thu.'),
    ('LOW_SERVICE_USAGE', 'Dịch vụ ít được sử dụng', 'SERVICE', 5, 'LESS_THAN', 'WARNING', TRUE,
     'Một dịch vụ có tỷ lệ sử dụng thấp hơn 5%.'),
    ('HIGH_VALUE_SERVICE', 'Dịch vụ giá trị cao', 'SERVICE', 25, 'GREATER_THAN', 'OPPORTUNITY', TRUE,
     'Một dịch vụ có ít đơn nhưng đóng góp doanh thu cao.'),
    ('HIGH_RETURNING_CUSTOMER_RATE', 'Khách quay lại cao', 'CUSTOMER', 50, 'GREATER_THAN', 'POSITIVE', TRUE,
     'Khách hàng quay lại chiếm hơn 50% tổng số đơn.'),
    ('LOW_RETURNING_CUSTOMER_RATE', 'Khách quay lại thấp', 'CUSTOMER', 25, 'LESS_THAN', 'WARNING', TRUE,
     'Tỷ lệ khách hàng quay lại thấp hơn 25%.'),
    ('HIGH_VALUE_CUSTOMER_GROUP', 'Nhóm khách giá trị cao', 'CUSTOMER', 50, 'GREATER_THAN', 'OPPORTUNITY', TRUE,
     'Nhóm khách hàng giá trị cao đóng góp hơn 50% doanh thu.'),
    ('LOW_POINT_REDEMPTION', 'Tỷ lệ sử dụng điểm thấp', 'LOYALTY', 20, 'LESS_THAN', 'WARNING', TRUE,
     'Tỷ lệ sử dụng điểm thấp hơn 20% tổng điểm đã tích.'),
    ('LOYALTY_UNUSED_POINTS', 'Khách có điểm nhưng ít dùng', 'LOYALTY', 10, 'LESS_THAN', 'WARNING', TRUE,
     'Tỷ lệ khách có điểm khả dụng phát sinh đổi điểm thấp.'),
    ('LOYALTY_EFFECTIVE', 'Loyalty hỗ trợ giữ chân khách', 'LOYALTY', 10, 'GREATER_THAN', 'POSITIVE', TRUE,
     'Khách có sử dụng điểm quay lại nhiều hơn khách không sử dụng điểm.')
ON CONFLICT (rule_code) DO UPDATE
SET rule_name = EXCLUDED.rule_name,
    type = EXCLUDED.type,
    threshold_value = EXCLUDED.threshold_value,
    comparison_operator = EXCLUDED.comparison_operator,
    severity = EXCLUDED.severity,
    description = EXCLUDED.description,
    updated_at = NOW();

CREATE TABLE insight_ai_enrichment (
    id SERIAL PRIMARY KEY,
    business_insight_id INTEGER NOT NULL,
    ai_summary TEXT,
    ai_explanation TEXT,
    ai_recommendation TEXT,
    ai_campaign_suggestion TEXT,
    confidence_score DECIMAL(5,2),
    ai_model VARCHAR(100),
    prompt_version VARCHAR(50),
    generated_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_insight_ai_enrichment_business_insight
        FOREIGN KEY (business_insight_id)
        REFERENCES business_insight(insight_id)
        ON DELETE CASCADE,

    CONSTRAINT uk_insight_ai_enrichment_business_insight
        UNIQUE (business_insight_id)
);

CREATE OR REPLACE FUNCTION trigger_set_timestamp_insight_ai_enrichment()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER set_timestamp_insight_ai_enrichment
BEFORE UPDATE ON insight_ai_enrichment
FOR EACH ROW
EXECUTE FUNCTION trigger_set_timestamp_insight_ai_enrichment();

ALTER TABLE insight_rule_config ENABLE ROW LEVEL SECURITY;
ALTER TABLE business_insight ENABLE ROW LEVEL SECURITY;
ALTER TABLE insight_ai_enrichment ENABLE ROW LEVEL SECURITY;

REVOKE ALL PRIVILEGES ON TABLE insight_rule_config FROM PUBLIC;
REVOKE ALL PRIVILEGES ON TABLE business_insight FROM PUBLIC;
REVOKE ALL PRIVILEGES ON TABLE insight_ai_enrichment FROM PUBLIC;
REVOKE ALL PRIVILEGES ON SEQUENCE insight_rule_config_rule_config_id_seq FROM PUBLIC;
REVOKE ALL PRIVILEGES ON SEQUENCE business_insight_insight_id_seq FROM PUBLIC;
REVOKE ALL PRIVILEGES ON SEQUENCE insight_ai_enrichment_id_seq FROM PUBLIC;
REVOKE EXECUTE ON FUNCTION trigger_set_timestamp_insight_ai_enrichment() FROM PUBLIC;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'anon') THEN
        REVOKE ALL PRIVILEGES ON TABLE insight_rule_config FROM anon;
        REVOKE ALL PRIVILEGES ON TABLE business_insight FROM anon;
        REVOKE ALL PRIVILEGES ON TABLE insight_ai_enrichment FROM anon;
        REVOKE ALL PRIVILEGES ON SEQUENCE insight_rule_config_rule_config_id_seq FROM anon;
        REVOKE ALL PRIVILEGES ON SEQUENCE business_insight_insight_id_seq FROM anon;
        REVOKE ALL PRIVILEGES ON SEQUENCE insight_ai_enrichment_id_seq FROM anon;
        REVOKE EXECUTE ON FUNCTION trigger_set_timestamp_insight_ai_enrichment() FROM anon;
    END IF;

    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authenticated') THEN
        REVOKE ALL PRIVILEGES ON TABLE insight_rule_config FROM authenticated;
        REVOKE ALL PRIVILEGES ON TABLE business_insight FROM authenticated;
        REVOKE ALL PRIVILEGES ON TABLE insight_ai_enrichment FROM authenticated;
        REVOKE ALL PRIVILEGES ON SEQUENCE insight_rule_config_rule_config_id_seq FROM authenticated;
        REVOKE ALL PRIVILEGES ON SEQUENCE business_insight_insight_id_seq FROM authenticated;
        REVOKE ALL PRIVILEGES ON SEQUENCE insight_ai_enrichment_id_seq FROM authenticated;
        REVOKE EXECUTE ON FUNCTION trigger_set_timestamp_insight_ai_enrichment() FROM authenticated;
    END IF;
END
$$;

CREATE INDEX IF NOT EXISTS idx_booking_insight_booking_date
    ON booking(booking_date);

CREATE INDEX IF NOT EXISTS idx_invoice_insight_paid_status_date
    ON invoice(status, paid_at)
    WHERE paid_at IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_loyalty_transaction_insight_created_at
    ON loyalty_transaction(created_at);

CREATE INDEX IF NOT EXISTS idx_loyalty_account_insight_available_points
    ON loyalty_account(status, available_points)
    WHERE available_points > 0;

DO $$
BEGIN
    IF to_regclass('public.reward_redemption') IS NOT NULL THEN
        CREATE INDEX IF NOT EXISTS idx_reward_redemption_insight_redeemed_at
            ON reward_redemption(redeemed_at);
    END IF;
END
$$;
