-- Tier-based booking window: mỗi hạng được đặt lịch trước bao nhiêu ngày.
-- Hạng cao đặt xa hơn → ưu tiên giành slot sớm hơn (Priority queue theo đề).
ALTER TABLE membership_tier ADD COLUMN IF NOT EXISTS advance_booking_days INTEGER;

-- Seed theo tên hạng (khớp cả tên tiếng Anh lẫn tiếng Việt). Chỉ set khi còn NULL
-- nên chạy lại an toàn và không đè giá trị Admin đã chỉnh.
UPDATE membership_tier SET advance_booking_days = 10
    WHERE advance_booking_days IS NULL
      AND (tier_name ILIKE '%silver%' OR tier_name ILIKE '%bạc%' OR tier_name ILIKE '%bac%');

UPDATE membership_tier SET advance_booking_days = 12
    WHERE advance_booking_days IS NULL
      AND (tier_name ILIKE '%gold%' OR tier_name ILIKE '%vàng%' OR tier_name ILIKE '%vang%');

UPDATE membership_tier SET advance_booking_days = 14
    WHERE advance_booking_days IS NULL
      AND (tier_name ILIKE '%platinum%' OR tier_name ILIKE '%bạch kim%' OR tier_name ILIKE '%bach kim%'
           OR tier_name ILIKE '%diamond%' OR tier_name ILIKE '%kim cương%' OR tier_name ILIKE '%kim cuong%');

-- Các hạng còn lại (Member/Thành viên/Đồng...) dùng cửa sổ cơ bản 7 ngày.
UPDATE membership_tier SET advance_booking_days = 7 WHERE advance_booking_days IS NULL;
