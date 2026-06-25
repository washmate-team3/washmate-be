-- Re-apply the analytics view in a forward migration so existing databases
-- that already baselined older migrations still get the AI/analytics source.
CREATE OR REPLACE VIEW booking_behavior_monthly AS
WITH booking_month AS (
    SELECT
        b.booking_id,
        b.user_id,
        b.garage_id,
        b.slot_id,
        b.status,
        TO_CHAR(b.booking_date, 'YYYY-MM') AS month_year,
        COALESCE(b.updated_at, b.created_at) AS activity_at
    FROM booking b
    WHERE b.deleted_at IS NULL
)
SELECT
    bm.user_id,
    bm.garage_id,
    bm.month_year,
    COUNT(*)::INT AS total_bookings,
    COUNT(*) FILTER (WHERE bm.status = 'COMPLETED')::INT AS completed_count,
    COUNT(*) FILTER (WHERE bm.status = 'CANCELLED')::INT AS cancelled_count,
    COUNT(*) FILTER (WHERE bm.status = 'NO_SHOW')::INT AS no_show_count,
    COALESCE(SUM(p.amount) FILTER (WHERE p.status = 'PAID'), 0)::DECIMAL(12,2) AS total_spent,
    (
        SELECT ranked.slot_id
        FROM (
            SELECT bm2.slot_id, COUNT(*) AS usage_count
            FROM booking_month bm2
            WHERE bm2.user_id = bm.user_id
              AND bm2.garage_id = bm.garage_id
              AND bm2.month_year = bm.month_year
            GROUP BY bm2.slot_id
            ORDER BY usage_count DESC, bm2.slot_id
            LIMIT 1
        ) ranked
    ) AS preferred_slot_id,
    'ACTIVE'::VARCHAR(20) AS status,
    MAX(bm.activity_at) AS last_updated
FROM booking_month bm
LEFT JOIN payment p ON p.booking_id = bm.booking_id
GROUP BY bm.user_id, bm.garage_id, bm.month_year;
