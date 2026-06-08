CREATE TABLE IF NOT EXISTS garage (
    garage_id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    address VARCHAR(500) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','INACTIVE','DELETED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS app_user (
    user_id SERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE,
    password_hash VARCHAR(255),
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(20) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','INACTIVE','BLOCKED','DELETED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS role (
    role_id SERIAL PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL UNIQUE CHECK (role_name IN ('CUSTOMER','STAFF','ADMIN','OWNER')),
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','INACTIVE','DELETED'))
);

CREATE TABLE IF NOT EXISTS user_role (
    user_role_id SERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES app_user(user_id),
    role_id INT NOT NULL REFERENCES role(role_id),
    garage_id INT REFERENCES garage(garage_id),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','INACTIVE','DELETED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_user_role_per_garage ON user_role(user_id, role_id, garage_id) WHERE garage_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_user_role_global ON user_role(user_id, role_id) WHERE garage_id IS NULL;
CREATE INDEX IF NOT EXISTS idx_user_role_user_garage ON user_role(user_id, garage_id, status);

CREATE TABLE IF NOT EXISTS vehicle (
    vehicle_id SERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES app_user(user_id),
    license_plate VARCHAR(30) NOT NULL,
    brand VARCHAR(100),
    model VARCHAR(100),
    color VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','INACTIVE','DELETED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    UNIQUE (vehicle_id, user_id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_active_vehicle_plate ON vehicle(license_plate) WHERE status = 'ACTIVE';
CREATE INDEX IF NOT EXISTS idx_vehicle_user ON vehicle(user_id);

CREATE TABLE IF NOT EXISTS booking_slot (
    slot_id SERIAL PRIMARY KEY,
    garage_id INT NOT NULL REFERENCES garage(garage_id),
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    max_capacity INT NOT NULL DEFAULT 4 CHECK (max_capacity > 0),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','INACTIVE','DELETED')),
    UNIQUE (slot_id, garage_id),
    UNIQUE (garage_id, start_time, end_time),
    CHECK (start_time < end_time)
);

CREATE TABLE IF NOT EXISTS service_package (
    service_id SERIAL PRIMARY KEY,
    garage_id INT NOT NULL REFERENCES garage(garage_id),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL CHECK (price >= 0),
    duration INT NOT NULL CHECK (duration > 0),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','INACTIVE','DELETED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    UNIQUE (service_id, garage_id),
    UNIQUE (garage_id, name)
);

CREATE TABLE IF NOT EXISTS booking (
    booking_id SERIAL PRIMARY KEY,
    booking_code VARCHAR(50) NOT NULL UNIQUE,
    user_id INT NOT NULL REFERENCES app_user(user_id),
    garage_id INT NOT NULL REFERENCES garage(garage_id),
    slot_id INT NOT NULL,
    service_id INT NOT NULL,
    vehicle_id INT NOT NULL,
    booking_date DATE NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL CHECK (total_amount >= 0),
    discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0 CHECK (discount_amount >= 0),
    final_amount DECIMAL(10,2) NOT NULL CHECK (final_amount >= 0),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','CONFIRMED','CHECKED_IN','WASHING','COMPLETED','CANCELLED','NO_SHOW')),
    assigned_staff_user_id INT REFERENCES app_user(user_id),
    confirmed_at TIMESTAMPTZ,
    checkin_time TIMESTAMPTZ,
    service_start_time TIMESTAMPTZ,
    completed_time TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    no_show_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    UNIQUE (booking_id, garage_id),
    FOREIGN KEY (slot_id, garage_id) REFERENCES booking_slot(slot_id, garage_id),
    FOREIGN KEY (service_id, garage_id) REFERENCES service_package(service_id, garage_id),
    FOREIGN KEY (vehicle_id, user_id) REFERENCES vehicle(vehicle_id, user_id),
    CHECK (final_amount = total_amount - discount_amount)
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_active_vehicle_booking ON booking(vehicle_id, slot_id, booking_date) WHERE status IN ('PENDING','CONFIRMED','CHECKED_IN','WASHING');
CREATE INDEX IF NOT EXISTS idx_booking_slot_date_status ON booking(slot_id, garage_id, booking_date, status);
CREATE INDEX IF NOT EXISTS idx_booking_user ON booking(user_id);
CREATE INDEX IF NOT EXISTS idx_booking_garage_date ON booking(garage_id, booking_date);

CREATE TABLE IF NOT EXISTS booking_slot_capacity (
    slot_id INT NOT NULL,
    garage_id INT NOT NULL,
    booking_date DATE NOT NULL,
    current_capacity INT NOT NULL DEFAULT 0 CHECK (current_capacity >= 0),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (slot_id, booking_date),
    FOREIGN KEY (slot_id, garage_id) REFERENCES booking_slot(slot_id, garage_id)
);

CREATE TABLE IF NOT EXISTS payment (
    payment_id SERIAL PRIMARY KEY,
    booking_id INT NOT NULL UNIQUE,
    garage_id INT NOT NULL REFERENCES garage(garage_id),
    amount DECIMAL(10,2) NOT NULL CHECK (amount >= 0),
    method VARCHAR(30) NOT NULL DEFAULT 'CASH' CHECK (method IN ('CASH','VNPAY','MOMO','BANK_TRANSFER','CARD')),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','PAID','FAILED','CANCELLED','REFUNDED')),
    paid_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    FOREIGN KEY (booking_id, garage_id) REFERENCES booking(booking_id, garage_id)
);
CREATE INDEX IF NOT EXISTS idx_payment_booking ON payment(booking_id);

CREATE TABLE IF NOT EXISTS payment_transaction (
    payment_transaction_id SERIAL PRIMARY KEY,
    payment_id INT NOT NULL REFERENCES payment(payment_id),
    provider VARCHAR(50),
    provider_txn_id VARCHAR(255),
    amount DECIMAL(10,2) NOT NULL CHECK (amount >= 0),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','SUCCESS','FAILED','CANCELLED','REFUNDED')),
    raw_response JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_provider_txn_idempotency ON payment_transaction(provider, provider_txn_id) WHERE provider IS NOT NULL AND provider_txn_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS invoice (
    invoice_id SERIAL PRIMARY KEY,
    invoice_code VARCHAR(50) NOT NULL UNIQUE,
    booking_id INT NOT NULL UNIQUE,
    payment_id INT REFERENCES payment(payment_id),
    garage_id INT NOT NULL REFERENCES garage(garage_id),
    subtotal DECIMAL(10,2) NOT NULL DEFAULT 0 CHECK (subtotal >= 0),
    discount DECIMAL(10,2) NOT NULL DEFAULT 0 CHECK (discount >= 0),
    penalty_total DECIMAL(10,2) NOT NULL DEFAULT 0 CHECK (penalty_total >= 0),
    total_amount DECIMAL(10,2) NOT NULL CHECK (total_amount >= 0),
    status VARCHAR(30) NOT NULL DEFAULT 'ISSUED' CHECK (status IN ('ISSUED','PAID','CANCELLED','REFUNDED')),
    issued_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    paid_at TIMESTAMPTZ,
    FOREIGN KEY (booking_id, garage_id) REFERENCES booking(booking_id, garage_id),
    CHECK (total_amount = subtotal - discount + penalty_total)
);
CREATE INDEX IF NOT EXISTS idx_invoice_booking ON invoice(booking_id);

CREATE TABLE IF NOT EXISTS loyalty_policy (
    policy_id SERIAL PRIMARY KEY,
    garage_id INT NOT NULL UNIQUE REFERENCES garage(garage_id),
    amount_per_point DECIMAL(10,2) NOT NULL DEFAULT 10000 CHECK (amount_per_point > 0),
    point_expiry_months INT NOT NULL DEFAULT 12 CHECK (point_expiry_months > 0),
    auto_enroll BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','INACTIVE','DELETED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS membership_tier (
    tier_id SERIAL PRIMARY KEY,
    garage_id INT NOT NULL REFERENCES garage(garage_id),
    tier_name VARCHAR(50) NOT NULL,
    min_points INT NOT NULL DEFAULT 0 CHECK (min_points >= 0),
    discount_percentage DECIMAL(5,2) NOT NULL DEFAULT 0 CHECK (discount_percentage BETWEEN 0 AND 100),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','INACTIVE','DELETED')),
    UNIQUE (tier_id, garage_id),
    UNIQUE (garage_id, tier_name),
    UNIQUE (garage_id, min_points)
);

INSERT INTO role(role_name, description, status) VALUES
    ('CUSTOMER', 'Customer role', 'ACTIVE'),
    ('STAFF', 'Staff role', 'ACTIVE'),
    ('ADMIN', 'Admin role', 'ACTIVE'),
    ('OWNER', 'Owner role', 'ACTIVE')
ON CONFLICT (role_name) DO NOTHING;
