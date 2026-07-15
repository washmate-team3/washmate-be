-- Đảm bảo insight_rule_config có ĐỦ mọi rule_code mà rule engine phát ra.
--
-- Lý do: một số DB được seed thiếu (dựng từ dump cũ), khiến khi generate insight,
-- INSERT vào business_insight với rule_code chưa có trong insight_rule_config bị
-- vi phạm khóa ngoại fk_business_insight_rule (vd ORDER_CANCEL_RATE_HIGH).
--
-- ON CONFLICT (rule_code) DO NOTHING: giữ nguyên cấu hình đã có, chỉ chèn dòng còn thiếu.
-- Chạy sau V3 (đã resync sequence) nên INSERT mới không đụng khóa chính.

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
     'Khách có sử dụng điểm quay lại nhiều hơn khách không sử dụng điểm.'),
    ('POINTS_EXPIRING_SOON', 'Điểm của khách sắp hết hạn', 'LOYALTY', 100, 'GREATER_THAN', 'WARNING', TRUE,
     'Cảnh báo khi có nhiều điểm sắp hết hạn trong 30 ngày tới.'),
    ('UPGRADE_STALL', 'Khách sắp đủ điểm lên hạng', 'LOYALTY', 3, 'GREATER_THAN_OR_EQUAL', 'OPPORTUNITY', TRUE,
     'Cảnh báo khi có từ 3 khách trở lên chỉ còn thiếu ít điểm là lên hạng.'),
    ('DOWNGRADE_WAVE', 'Tỷ lệ hạ hạng cao', 'LOYALTY', 15, 'GREATER_THAN', 'WARNING', TRUE,
     'Cảnh báo khi tỷ lệ tài khoản bị hạ hạng trong kỳ vượt 15%.'),
    ('TIER_DISTRIBUTION_SKEW', 'Phần lớn khách kẹt hạng thấp', 'LOYALTY', 80, 'GREATER_THAN', 'WARNING', TRUE,
     'Cảnh báo khi trên 80% khách kẹt ở hạng thấp nhất.')
ON CONFLICT (rule_code) DO NOTHING;
