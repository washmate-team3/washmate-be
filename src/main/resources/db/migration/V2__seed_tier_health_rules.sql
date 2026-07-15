-- Seed 4 rule "sức khỏe tier/loyalty" bám khung đề SU26SWP01.
-- Idempotent: chạy lại an toàn nhờ ON CONFLICT. Các rule vẫn hoạt động kể cả khi
-- chưa có dòng config (mặc định active + ngưỡng lấy từ InsightThresholds trong code);
-- seed này để rule hiện trên trang quản lý và mang theo "căn cứ" ngưỡng.

-- Đồng bộ lại sequence của khóa chính trước khi insert: baseline có thể đã nạp dữ liệu
-- mà không đẩy sequence, khiến INSERT mới xin trùng rule_config_id (lỗi insight_rule_config_pkey).
SELECT setval(
    pg_get_serial_sequence('insight_rule_config', 'rule_config_id'),
    GREATEST(COALESCE((SELECT MAX(rule_config_id) FROM insight_rule_config), 0), 1)
);

INSERT INTO insight_rule_config(rule_code, rule_name, type, threshold_value, comparison_operator, severity, is_active, description)
VALUES
    ('POINTS_EXPIRING_SOON', 'Điểm của khách sắp hết hạn', 'LOYALTY', 100, 'GREATER_THAN', 'WARNING', TRUE,
     'Cảnh báo khi có nhiều điểm sắp hết hạn trong 30 ngày tới. Căn cứ: 30 ngày đủ 1 chu kỳ rửa (~4 tuần) để khách quay lại đổi điểm trước khi mất. Gắn yêu cầu "Points expire after 12 months" trong đề.'),
    ('UPGRADE_STALL', 'Khách sắp đủ điểm lên hạng', 'LOYALTY', 3, 'GREATER_THAN_OR_EQUAL', 'OPPORTUNITY', TRUE,
     'Cảnh báo khi có từ 3 khách trở lên chỉ còn thiếu trong khoảng 50 điểm là lên hạng. Căn cứ: khoảng cách ~1-2 lần rửa nữa, dễ kích hoạt bằng nudge.'),
    ('DOWNGRADE_WAVE', 'Tỷ lệ hạ hạng cao', 'LOYALTY', 15, 'GREATER_THAN', 'WARNING', TRUE,
     'Cảnh báo khi tỷ lệ tài khoản bị hạ hạng trong kỳ vượt 15% tổng tài khoản active. Căn cứ: hạ hạng hàng loạt = điểm duy trì hạng quá cao, cần xem lại policy. Gắn yêu cầu "auto-downgrade" trong đề.'),
    ('TIER_DISTRIBUTION_SKEW', 'Phần lớn khách kẹt hạng thấp', 'LOYALTY', 80, 'GREATER_THAN', 'WARNING', TRUE,
     'Cảnh báo khi trên 80% khách kẹt ở hạng thấp nhất. Căn cứ: hệ multi-tier mất ý nghĩa nếu gần như không ai thăng hạng. Gắn yêu cầu "Tiered benefits (Silver/Gold/Platinum)" trong đề.')
ON CONFLICT (rule_code) DO UPDATE
SET rule_name = EXCLUDED.rule_name,
    type = EXCLUDED.type,
    threshold_value = EXCLUDED.threshold_value,
    comparison_operator = EXCLUDED.comparison_operator,
    severity = EXCLUDED.severity,
    description = EXCLUDED.description,
    updated_at = NOW();
