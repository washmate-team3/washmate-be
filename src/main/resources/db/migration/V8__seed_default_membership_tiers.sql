INSERT INTO membership_tier (garage_id, tier_name, min_points, discount_percentage, status)
SELECT garage_id, 'Bronze', 0, 0, 'ACTIVE'
FROM garage
WHERE status = 'ACTIVE'
ON CONFLICT (garage_id, min_points) DO UPDATE
SET tier_name = EXCLUDED.tier_name,
    discount_percentage = EXCLUDED.discount_percentage,
    status = 'ACTIVE';
