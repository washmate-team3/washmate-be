-- Seed rule INACTIVE_WINBACK (khách lâu chưa quay lại) vào insight_rule_config.
-- Cần có để khi generate insight, INSERT business_insight với rule_code này không vi phạm khóa ngoại.
-- Idempotent: ON CONFLICT DO NOTHING.
INSERT INTO insight_rule_config(rule_code, rule_name, type, threshold_value, comparison_operator, severity, is_active, description)
VALUES
    ('INACTIVE_WINBACK', 'Nhiều khách lâu chưa quay lại', 'CUSTOMER', 3, 'GREATER_THAN_OR_EQUAL', 'WARNING', TRUE,
     'Cảnh báo khi có từ 3 khách trở lên đã hơn 45 ngày không đặt lịch. Căn cứ: chu kỳ rửa ~4 tuần, quá 6 tuần là nguy cơ rời bỏ; nối tới chiến dịch win-back (email + voucher).')
ON CONFLICT (rule_code) DO NOTHING;
