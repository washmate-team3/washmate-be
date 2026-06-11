INSERT INTO role (role_id, role_name, description, status) VALUES
    (1, 'CUSTOMER', 'Customer role', 'ACTIVE'),
    (2, 'STAFF', 'Staff role', 'ACTIVE'),
    (3, 'MANAGER', 'Manager role', 'ACTIVE'),
    (4, 'ADMIN', 'Admin role', 'ACTIVE'),
    (5, 'OWNER', 'Owner role', 'ACTIVE');

INSERT INTO app_user (
    user_id,
    email,
    password_hash,
    full_name,
    phone,
    status,
    created_at
) VALUES
    (1, 'customer1@gmail.com', '$2a$10$AqvfRFgL7GFeBp1/9j055.GHCtqEGqkS72O9kcht3Zo38xXBnoX8G', 'Demo Customer', '0900000001', 'ACTIVE', CURRENT_TIMESTAMP),
    (2, 'manager1@gmail.com', '$2a$10$AqvfRFgL7GFeBp1/9j055.GHCtqEGqkS72O9kcht3Zo38xXBnoX8G', 'Demo Manager', '0900000002', 'ACTIVE', CURRENT_TIMESTAMP);

INSERT INTO garage (
    garage_id,
    name,
    address,
    phone,
    status,
    created_at
) VALUES
    (1, 'WashMate Demo Garage', '123 Demo Street, Ho Chi Minh City', '02800000001', 'ACTIVE', CURRENT_TIMESTAMP);

INSERT INTO user_role (user_role_id, user_id, role_id, garage_id, status, created_at) VALUES
    (1, 1, 1, NULL, 'ACTIVE', CURRENT_TIMESTAMP),
    (2, 2, 3, 1, 'ACTIVE', CURRENT_TIMESTAMP);

INSERT INTO booking_slot (
    slot_id,
    garage_id,
    start_time,
    end_time,
    max_capacity,
    status
) VALUES
    (1, 1, TIME '08:00:00', TIME '09:00:00', 4, 'ACTIVE');

INSERT INTO service_package (
    service_id,
    garage_id,
    name,
    description,
    price,
    duration,
    status,
    created_at
) VALUES
    (1, 1, 'Basic Wash', 'Demo exterior wash package', 120000.00, 45, 'ACTIVE', CURRENT_TIMESTAMP);

INSERT INTO vehicle (
    vehicle_id,
    user_id,
    license_plate,
    brand,
    model,
    color,
    status,
    created_at
) VALUES
    (3, 1, '51A-12345', 'Toyota', 'Vios', 'White', 'ACTIVE', CURRENT_TIMESTAMP);

INSERT INTO membership_tier (
    tier_id,
    garage_id,
    tier_name,
    min_points,
    discount_percentage,
    status
) VALUES
    (1, 1, 'Bronze', 0, 0.00, 'ACTIVE');

ALTER TABLE role ALTER COLUMN role_id RESTART WITH 6;
ALTER TABLE app_user ALTER COLUMN user_id RESTART WITH 3;
ALTER TABLE garage ALTER COLUMN garage_id RESTART WITH 2;
ALTER TABLE user_role ALTER COLUMN user_role_id RESTART WITH 3;
ALTER TABLE booking_slot ALTER COLUMN slot_id RESTART WITH 2;
ALTER TABLE service_package ALTER COLUMN service_id RESTART WITH 2;
ALTER TABLE vehicle ALTER COLUMN vehicle_id RESTART WITH 4;
ALTER TABLE membership_tier ALTER COLUMN tier_id RESTART WITH 2;
