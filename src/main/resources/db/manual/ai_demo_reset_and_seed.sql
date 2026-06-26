-- AI Customer Insights demo data - clean reset version.
-- Run this whole file in Supabase SQL Editor.
--
-- After it finishes, copy the returned garage_id and call:
-- POST /api/admin/insights/generate?garageId=<garage_id>&period=2026-06
--
-- Expected result after generate:
-- 5 customers x 4 insight types = 20 rows in customer_ai_insight.

BEGIN;

-- 1) Clean only this demo seed.
DELETE FROM customer_ai_insight
WHERE garage_id IN (SELECT garage_id FROM garage WHERE name = 'AI Demo Garage')
   OR user_id IN (SELECT user_id FROM app_user WHERE phone LIKE '0988000%');

DELETE FROM payment
WHERE booking_id IN (
    SELECT booking_id FROM booking WHERE booking_code LIKE 'AI-DEMO-%'
);

DELETE FROM booking WHERE booking_code LIKE 'AI-DEMO-%';
DELETE FROM vehicle WHERE license_plate LIKE 'AIDEMO-%';

DELETE FROM user_role
WHERE user_id IN (SELECT user_id FROM app_user WHERE phone LIKE '0988000%')
   OR garage_id IN (SELECT garage_id FROM garage WHERE name = 'AI Demo Garage');

DELETE FROM app_user WHERE phone LIKE '0988000%';
DELETE FROM service_package WHERE garage_id IN (SELECT garage_id FROM garage WHERE name = 'AI Demo Garage');
DELETE FROM booking_slot WHERE garage_id IN (SELECT garage_id FROM garage WHERE name = 'AI Demo Garage');
DELETE FROM garage WHERE name = 'AI Demo Garage';

-- 2) Base garage, users, roles, slots, services, vehicles.
INSERT INTO garage(name, address, phone, status)
VALUES ('AI Demo Garage', '123 Demo Street, HCMC', '0988000000', 'ACTIVE');

INSERT INTO app_user(email, password_hash, full_name, phone, status)
VALUES
    ('ai.admin.demo@washmate.test', NULL, 'AI Demo Manager', '0988000001', 'ACTIVE'),
    ('ai.vip.demo@washmate.test', NULL, 'VIP Customer Demo', '0988000002', 'ACTIVE'),
    ('ai.risk.demo@washmate.test', NULL, 'At Risk Customer Demo', '0988000003', 'ACTIVE'),
    ('ai.loyal.demo@washmate.test', NULL, 'Loyal Customer Demo', '0988000004', 'ACTIVE'),
    ('ai.new.demo@washmate.test', NULL, 'New Customer Demo', '0988000005', 'ACTIVE'),
    ('ai.regular.demo@washmate.test', NULL, 'Regular Customer Demo', '0988000006', 'ACTIVE');

INSERT INTO user_role(user_id, role_id, garage_id, status)
SELECT u.user_id, r.role_id, g.garage_id, 'ACTIVE'
FROM app_user u
JOIN garage g ON g.name = 'AI Demo Garage'
JOIN role r ON (
    (u.phone = '0988000001' AND r.role_name = 'MANAGER')
    OR (u.phone <> '0988000001' AND r.role_name = 'CUSTOMER')
)
WHERE u.phone LIKE '0988000%';

INSERT INTO booking_slot(garage_id, start_time, end_time, max_capacity, status)
SELECT g.garage_id, x.start_time, x.end_time, 20, 'ACTIVE'
FROM garage g
CROSS JOIN (
    VALUES
        (TIME '08:00', TIME '10:00'),
        (TIME '14:00', TIME '16:00')
) AS x(start_time, end_time)
WHERE g.name = 'AI Demo Garage';

INSERT INTO service_package(garage_id, name, description, price, duration, status)
SELECT g.garage_id, x.name, x.description, x.price, x.duration, 'ACTIVE'
FROM garage g
CROSS JOIN (
    VALUES
        ('AI Demo Basic Wash', 'Demo basic car wash', 150000::DECIMAL(10,2), 45),
        ('AI Demo Premium Detail', 'Demo premium detailing', 300000::DECIMAL(10,2), 90)
) AS x(name, description, price, duration)
WHERE g.name = 'AI Demo Garage';

INSERT INTO vehicle(user_id, license_plate, brand, model, color, status)
SELECT u.user_id, x.license_plate, x.brand, x.model, x.color, 'ACTIVE'
FROM app_user u
JOIN (
    VALUES
        ('0988000002', 'AIDEMO-VIP', 'Mercedes', 'C300', 'Black'),
        ('0988000003', 'AIDEMO-RISK', 'Toyota', 'Vios', 'White'),
        ('0988000004', 'AIDEMO-LOYAL', 'Honda', 'City', 'Blue'),
        ('0988000005', 'AIDEMO-NEW', 'Kia', 'Morning', 'Red'),
        ('0988000006', 'AIDEMO-REGULAR', 'Mazda', '3', 'Gray')
) AS x(phone, license_plate, brand, model, color) ON x.phone = u.phone;

-- 3) Demo bookings for period 2026-06.
WITH seed AS (
    SELECT *
    FROM (
        VALUES
            -- VIP: completed_count >= 5 and high spending.
            ('AI-DEMO-VIP-01', '0988000002', 'AIDEMO-VIP', TIME '08:00', 'AI Demo Premium Detail', DATE '2026-06-01', 300000::DECIMAL(10,2), 'COMPLETED', 'VNPAY', 'PAID'),
            ('AI-DEMO-VIP-02', '0988000002', 'AIDEMO-VIP', TIME '08:00', 'AI Demo Premium Detail', DATE '2026-06-04', 300000::DECIMAL(10,2), 'COMPLETED', 'VNPAY', 'PAID'),
            ('AI-DEMO-VIP-03', '0988000002', 'AIDEMO-VIP', TIME '08:00', 'AI Demo Premium Detail', DATE '2026-06-07', 300000::DECIMAL(10,2), 'COMPLETED', 'VNPAY', 'PAID'),
            ('AI-DEMO-VIP-04', '0988000002', 'AIDEMO-VIP', TIME '08:00', 'AI Demo Premium Detail', DATE '2026-06-10', 300000::DECIMAL(10,2), 'COMPLETED', 'VNPAY', 'PAID'),
            ('AI-DEMO-VIP-05', '0988000002', 'AIDEMO-VIP', TIME '08:00', 'AI Demo Premium Detail', DATE '2026-06-13', 300000::DECIMAL(10,2), 'COMPLETED', 'VNPAY', 'PAID'),

            -- AT_RISK: cancel + no-show >= 2.
            ('AI-DEMO-RISK-01', '0988000003', 'AIDEMO-RISK', TIME '14:00', 'AI Demo Basic Wash', DATE '2026-06-03', 150000::DECIMAL(10,2), 'COMPLETED', 'CASH', 'PAID'),
            ('AI-DEMO-RISK-02', '0988000003', 'AIDEMO-RISK', TIME '14:00', 'AI Demo Basic Wash', DATE '2026-06-09', 150000::DECIMAL(10,2), 'CANCELLED', 'CASH', 'CANCELLED'),
            ('AI-DEMO-RISK-03', '0988000003', 'AIDEMO-RISK', TIME '14:00', 'AI Demo Basic Wash', DATE '2026-06-15', 150000::DECIMAL(10,2), 'NO_SHOW', 'CASH', 'CANCELLED'),

            -- LOYAL: 3 completed, no cancel/no-show.
            ('AI-DEMO-LOYAL-01', '0988000004', 'AIDEMO-LOYAL', TIME '08:00', 'AI Demo Basic Wash', DATE '2026-06-02', 150000::DECIMAL(10,2), 'COMPLETED', 'VNPAY', 'PAID'),
            ('AI-DEMO-LOYAL-02', '0988000004', 'AIDEMO-LOYAL', TIME '08:00', 'AI Demo Basic Wash', DATE '2026-06-09', 150000::DECIMAL(10,2), 'COMPLETED', 'VNPAY', 'PAID'),
            ('AI-DEMO-LOYAL-03', '0988000004', 'AIDEMO-LOYAL', TIME '08:00', 'AI Demo Basic Wash', DATE '2026-06-16', 150000::DECIMAL(10,2), 'COMPLETED', 'VNPAY', 'PAID'),

            -- NEW: only one booking.
            ('AI-DEMO-NEW-01', '0988000005', 'AIDEMO-NEW', TIME '14:00', 'AI Demo Basic Wash', DATE '2026-06-18', 150000::DECIMAL(10,2), 'COMPLETED', 'CASH', 'PAID'),

            -- REGULAR with medium churn: 1 completed + 1 cancelled.
            ('AI-DEMO-REG-01', '0988000006', 'AIDEMO-REGULAR', TIME '08:00', 'AI Demo Basic Wash', DATE '2026-06-20', 150000::DECIMAL(10,2), 'COMPLETED', 'VNPAY', 'PAID'),
            ('AI-DEMO-REG-02', '0988000006', 'AIDEMO-REGULAR', TIME '08:00', 'AI Demo Basic Wash', DATE '2026-06-25', 150000::DECIMAL(10,2), 'CANCELLED', 'CASH', 'CANCELLED')
    ) AS x(booking_code, phone, license_plate, slot_start, service_name, booking_date, amount, booking_status, payment_method, payment_status)
),
inserted_bookings AS (
    INSERT INTO booking(
        booking_code,
        user_id,
        garage_id,
        slot_id,
        service_id,
        vehicle_id,
        booking_date,
        total_amount,
        discount_amount,
        final_amount,
        status,
        confirmed_at,
        checkin_time,
        service_start_time,
        completed_time,
        cancelled_at,
        no_show_at,
        created_at
    )
    SELECT
        s.booking_code,
        u.user_id,
        g.garage_id,
        bs.slot_id,
        sp.service_id,
        v.vehicle_id,
        s.booking_date,
        s.amount,
        0,
        s.amount,
        s.booking_status,
        CASE WHEN s.booking_status = 'COMPLETED' THEN (s.booking_date + TIME '08:05') AT TIME ZONE 'Asia/Ho_Chi_Minh' END,
        CASE WHEN s.booking_status = 'COMPLETED' THEN (s.booking_date + TIME '08:10') AT TIME ZONE 'Asia/Ho_Chi_Minh' END,
        CASE WHEN s.booking_status = 'COMPLETED' THEN (s.booking_date + TIME '08:15') AT TIME ZONE 'Asia/Ho_Chi_Minh' END,
        CASE WHEN s.booking_status = 'COMPLETED' THEN (s.booking_date + TIME '09:00') AT TIME ZONE 'Asia/Ho_Chi_Minh' END,
        CASE WHEN s.booking_status = 'CANCELLED' THEN ((s.booking_date - 1) + TIME '18:00') AT TIME ZONE 'Asia/Ho_Chi_Minh' END,
        CASE WHEN s.booking_status = 'NO_SHOW' THEN (s.booking_date + TIME '16:00') AT TIME ZONE 'Asia/Ho_Chi_Minh' END,
        ((s.booking_date - 1) + TIME '09:00') AT TIME ZONE 'Asia/Ho_Chi_Minh'
    FROM seed s
    JOIN app_user u ON u.phone = s.phone
    JOIN garage g ON g.name = 'AI Demo Garage'
    JOIN booking_slot bs ON bs.garage_id = g.garage_id AND bs.start_time = s.slot_start
    JOIN service_package sp ON sp.garage_id = g.garage_id AND sp.name = s.service_name
    JOIN vehicle v ON v.user_id = u.user_id AND v.license_plate = s.license_plate
    RETURNING booking_id, garage_id, booking_code, booking_date
)
INSERT INTO payment(booking_id, garage_id, amount, method, status, paid_at, created_at)
SELECT
    ib.booking_id,
    ib.garage_id,
    s.amount,
    s.payment_method,
    s.payment_status,
    CASE WHEN s.payment_status = 'PAID' THEN (ib.booking_date + TIME '09:05') AT TIME ZONE 'Asia/Ho_Chi_Minh' END,
    ((ib.booking_date - 1) + TIME '09:05') AT TIME ZONE 'Asia/Ho_Chi_Minh'
FROM inserted_bookings ib
JOIN seed s ON s.booking_code = ib.booking_code;

COMMIT;

-- 4) Copy this garage_id into the FE Garage input and API generate call.
SELECT garage_id, name
FROM garage
WHERE name = 'AI Demo Garage';

-- 5) Source data should show 5 rows before generate.
SELECT *
FROM booking_behavior_monthly
WHERE garage_id = (SELECT garage_id FROM garage WHERE name = 'AI Demo Garage')
  AND month_year = '2026-06'
ORDER BY user_id;
