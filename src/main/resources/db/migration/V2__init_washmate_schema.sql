-- Washmate Initial Schema (Consolidated from V2-V5)
-- ============================================================================
-- 1. Base Tables
-- ============================================================================

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
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('PENDING_VERIFY','ACTIVE','INACTIVE','BLOCKED','DELETED')),
    failed_login_count INT NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ,
    last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS role (
    role_id SERIAL PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL UNIQUE CHECK (role_name IN ('CUSTOMER','STAFF','MANAGER','ADMIN','OWNER')),
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','INACTIVE','DELETED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ
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
    expires_at TIMESTAMPTZ,
    paid_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    FOREIGN KEY (booking_id, garage_id) REFERENCES booking(booking_id, garage_id)
);
CREATE INDEX IF NOT EXISTS idx_payment_booking ON payment(booking_id);
CREATE INDEX IF NOT EXISTS idx_payment_status_expires_at ON payment(status, expires_at) WHERE expires_at IS NOT NULL;

CREATE TABLE IF NOT EXISTS payment_transaction (
    payment_transaction_id SERIAL PRIMARY KEY,
    payment_id INT NOT NULL REFERENCES payment(payment_id),
    provider VARCHAR(50),
    merchant_txn_ref VARCHAR(100),
    provider_txn_id VARCHAR(255),
    amount DECIMAL(10,2) NOT NULL CHECK (amount >= 0),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','SUCCESS','FAILED','CANCELLED','REFUNDED')),
    raw_response JSONB,
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_provider_txn_idempotency ON payment_transaction(provider, provider_txn_id) WHERE provider IS NOT NULL AND provider_txn_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_payment_txn_merchant_ref ON payment_transaction(provider, merchant_txn_ref) WHERE provider IS NOT NULL AND merchant_txn_ref IS NOT NULL;

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

CREATE TABLE IF NOT EXISTS loyalty_account (
    account_id       SERIAL PRIMARY KEY,
    user_id          INT NOT NULL REFERENCES app_user(user_id) ON DELETE RESTRICT,
    garage_id        INT NOT NULL REFERENCES garage(garage_id) ON DELETE RESTRICT,
    tier_id          INT NOT NULL,
    total_points     INT NOT NULL DEFAULT 0,
    available_points INT NOT NULL DEFAULT 0,
    status           VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ,

    CONSTRAINT uq_loyalty_user_garage UNIQUE (user_id, garage_id),
    CONSTRAINT uq_loyalty_account_garage UNIQUE (account_id, garage_id),
    CONSTRAINT fk_loyalty_tier_garage FOREIGN KEY (tier_id, garage_id) REFERENCES membership_tier(tier_id, garage_id),
    CONSTRAINT chk_loyalty_points CHECK (total_points >= 0 AND available_points >= 0 AND available_points <= total_points),
    CONSTRAINT chk_loyalty_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);
CREATE INDEX IF NOT EXISTS idx_loyalty_account_user_garage ON loyalty_account(user_id, garage_id);

CREATE TABLE IF NOT EXISTS loyalty_transaction (
    transaction_id        SERIAL PRIMARY KEY,
    account_id            INT NOT NULL REFERENCES loyalty_account(account_id) ON DELETE RESTRICT,
    booking_id            INT NULL REFERENCES booking(booking_id) ON DELETE RESTRICT,
    redemption_id         INT NULL,
    source_transaction_id INT NULL,
    points                INT NOT NULL,
    transaction_type      VARCHAR(30) NOT NULL,
    description           TEXT,
    earned_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at            TIMESTAMPTZ,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_loyalty_transaction_source FOREIGN KEY (source_transaction_id) REFERENCES loyalty_transaction(transaction_id) ON DELETE RESTRICT,
    CONSTRAINT chk_loyalty_transaction_type CHECK (transaction_type IN ('EARN', 'REDEEM', 'REFUND', 'EXPIRE', 'ADJUSTMENT', 'ROLLBACK')),
    CONSTRAINT chk_loyalty_transaction_points CHECK (points <> 0)
);
CREATE INDEX IF NOT EXISTS idx_loyalty_txn_account ON loyalty_transaction(account_id);
CREATE INDEX IF NOT EXISTS idx_loyalty_txn_booking_earn ON loyalty_transaction(booking_id) WHERE transaction_type = 'EARN';
CREATE INDEX IF NOT EXISTS idx_loyalty_txn_source ON loyalty_transaction(source_transaction_id);
CREATE INDEX IF NOT EXISTS idx_loyalty_txn_booking_type ON loyalty_transaction(booking_id, transaction_type);
CREATE INDEX IF NOT EXISTS idx_loyalty_txn_source_type ON loyalty_transaction(source_transaction_id, transaction_type);

CREATE TABLE IF NOT EXISTS loyalty_tier_history (
    history_id          SERIAL PRIMARY KEY,
    account_id          INT NOT NULL REFERENCES loyalty_account(account_id) ON DELETE RESTRICT,
    garage_id           INT NOT NULL REFERENCES garage(garage_id) ON DELETE RESTRICT,
    old_tier_id         INT NULL,
    new_tier_id         INT NOT NULL,
    changed_by_user_id  INT NULL REFERENCES app_user(user_id) ON DELETE SET NULL,
    change_reason       TEXT,
    change_type         VARCHAR(30) NOT NULL DEFAULT 'ADJUSTMENT',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_loyalty_tier_history_type CHECK (change_type IN ('UPGRADE', 'DOWNGRADE', 'ADJUSTMENT'))
);

CREATE TABLE IF NOT EXISTS penalty_fee (
    penalty_id   SERIAL PRIMARY KEY,
    booking_id   INT NOT NULL REFERENCES booking(booking_id) ON DELETE RESTRICT,
    garage_id    INT NOT NULL REFERENCES garage(garage_id) ON DELETE RESTRICT,
    penalty_type VARCHAR(50) NOT NULL,
    amount       DECIMAL(10,2) NOT NULL,
    reason       TEXT,
    status       VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_penalty_booking_garage FOREIGN KEY (booking_id, garage_id) REFERENCES booking(booking_id, garage_id) ON DELETE RESTRICT,
    CONSTRAINT chk_penalty_amount CHECK (amount >= 0),
    CONSTRAINT chk_penalty_type CHECK (penalty_type IN ('LATE_ARRIVAL', 'NO_SHOW', 'EXTRA_SERVICE', 'DAMAGE', 'OTHER')),
    CONSTRAINT chk_penalty_status CHECK (status IN ('ACTIVE', 'WAIVED', 'PAID', 'CANCELLED'))
);
CREATE INDEX IF NOT EXISTS idx_penalty_booking ON penalty_fee(booking_id);

CREATE TABLE IF NOT EXISTS otp_code (
    id SERIAL PRIMARY KEY,
    channel VARCHAR(20) NOT NULL DEFAULT 'EMAIL',
    identifier VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    code VARCHAR(255) NOT NULL,
    failed_attempts INT NOT NULL DEFAULT 0,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_otp_code_channel_identifier ON otp_code(channel, identifier);
CREATE INDEX IF NOT EXISTS idx_otp_code_channel_identifier ON otp_code(channel, identifier);

CREATE TABLE IF NOT EXISTS refresh_token (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES app_user(user_id) ON DELETE CASCADE,
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    replaced_by_token_hash VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_refresh_token_user ON refresh_token(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_token_expires_at ON refresh_token(expires_at);
CREATE INDEX IF NOT EXISTS idx_refresh_token_active_user ON refresh_token(user_id, expires_at) WHERE revoked_at IS NULL;

-- ============================================================================
-- 2. Initial Data
-- ============================================================================

INSERT INTO role(role_name, description, status) VALUES
    ('CUSTOMER', 'Customer role', 'ACTIVE'),
    ('STAFF', 'Staff role', 'ACTIVE'),
    ('MANAGER', 'Manager role', 'ACTIVE'),
    ('ADMIN', 'Admin role', 'ACTIVE'),
    ('OWNER', 'Owner role', 'ACTIVE')
ON CONFLICT (role_name) DO NOTHING;

-- ============================================================================
-- 3. Triggers and Functions
-- ============================================================================

-- Updated-at helpers
CREATE OR REPLACE FUNCTION set_updated_at() RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at := NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_garage_updated_at BEFORE UPDATE ON garage FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_app_user_updated_at BEFORE UPDATE ON app_user FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_role_updated_at BEFORE UPDATE ON role FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_vehicle_updated_at BEFORE UPDATE ON vehicle FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_service_package_updated_at BEFORE UPDATE ON service_package FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_loyalty_policy_updated_at BEFORE UPDATE ON loyalty_policy FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_loyalty_account_updated_at BEFORE UPDATE ON loyalty_account FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_booking_updated_at BEFORE UPDATE ON booking FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_payment_updated_at BEFORE UPDATE ON payment FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Booking guard and slot capacity
CREATE OR REPLACE FUNCTION validate_booking_date_guard() RETURNS TRIGGER AS $$
BEGIN
    IF NEW.status IN ('PENDING', 'CONFIRMED', 'CHECKED_IN', 'WASHING')
       AND NEW.booking_date < CURRENT_DATE THEN
        RAISE EXCEPTION 'booking_date cannot be in the past for active bookings';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validate_booking_date_guard
BEFORE INSERT OR UPDATE OF booking_date, status ON booking
FOR EACH ROW EXECUTE FUNCTION validate_booking_date_guard();

CREATE OR REPLACE FUNCTION validate_assigned_staff_role() RETURNS TRIGGER AS $$
DECLARE
    staff_role_count INT;
BEGIN
    IF NEW.assigned_staff_user_id IS NULL THEN RETURN NEW; END IF;
    SELECT COUNT(*) INTO staff_role_count FROM user_role ur
    JOIN role r ON ur.role_id = r.role_id JOIN app_user au ON au.user_id = ur.user_id
    WHERE ur.user_id = NEW.assigned_staff_user_id AND ur.garage_id = NEW.garage_id
      AND ur.status = 'ACTIVE' AND r.role_name = 'STAFF' AND r.status = 'ACTIVE' AND au.status = 'ACTIVE';
    IF staff_role_count = 0 THEN RAISE EXCEPTION 'assigned_staff_user_id must be an active STAFF in the same garage'; END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validate_assigned_staff_role
BEFORE INSERT OR UPDATE OF assigned_staff_user_id, garage_id ON booking
FOR EACH ROW EXECUTE FUNCTION validate_assigned_staff_role();

CREATE OR REPLACE FUNCTION check_booking_slot_capacity() RETURNS TRIGGER AS $$
DECLARE
    v_max_capacity INT;
    v_current_count INT;
BEGIN
    IF NEW.status NOT IN ('PENDING', 'CONFIRMED', 'CHECKED_IN', 'WASHING') THEN RETURN NEW; END IF;
    SELECT max_capacity INTO v_max_capacity FROM booking_slot
    WHERE slot_id = NEW.slot_id AND garage_id = NEW.garage_id AND status = 'ACTIVE' FOR UPDATE;
    IF v_max_capacity IS NULL THEN RAISE EXCEPTION 'Booking slot does not exist or is not active for this garage'; END IF;
    SELECT COUNT(*) INTO v_current_count FROM booking
    WHERE slot_id = NEW.slot_id AND garage_id = NEW.garage_id AND booking_date = NEW.booking_date
      AND status IN ('PENDING', 'CONFIRMED', 'CHECKED_IN', 'WASHING') AND (TG_OP = 'INSERT' OR booking_id <> NEW.booking_id);
    IF v_current_count >= v_max_capacity THEN RAISE EXCEPTION 'Selected booking slot is full for this date'; END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_check_booking_slot_capacity
BEFORE INSERT OR UPDATE OF status, slot_id, garage_id, booking_date ON booking
FOR EACH ROW EXECUTE FUNCTION check_booking_slot_capacity();

CREATE OR REPLACE FUNCTION refresh_one_slot_capacity(p_slot_id INT, p_garage_id INT, p_booking_date DATE) RETURNS VOID AS $$
DECLARE
    v_current_count INT;
BEGIN
    IF p_slot_id IS NULL OR p_garage_id IS NULL OR p_booking_date IS NULL THEN RETURN; END IF;
    SELECT COUNT(*) INTO v_current_count FROM booking
    WHERE slot_id = p_slot_id AND garage_id = p_garage_id AND booking_date = p_booking_date
      AND status IN ('PENDING', 'CONFIRMED', 'CHECKED_IN', 'WASHING');
    INSERT INTO booking_slot_capacity(slot_id, garage_id, booking_date, current_capacity, updated_at)
    VALUES (p_slot_id, p_garage_id, p_booking_date, v_current_count, NOW())
    ON CONFLICT (slot_id, booking_date)
    DO UPDATE SET garage_id = EXCLUDED.garage_id, current_capacity = EXCLUDED.current_capacity, updated_at = NOW();
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION refresh_booking_slot_capacity() RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        PERFORM refresh_one_slot_capacity(OLD.slot_id, OLD.garage_id, OLD.booking_date);
        RETURN OLD;
    END IF;
    IF TG_OP = 'UPDATE' THEN
        PERFORM refresh_one_slot_capacity(OLD.slot_id, OLD.garage_id, OLD.booking_date);
    END IF;
    PERFORM refresh_one_slot_capacity(NEW.slot_id, NEW.garage_id, NEW.booking_date);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_refresh_booking_slot_capacity_insert_update
AFTER INSERT OR UPDATE OF status, slot_id, garage_id, booking_date ON booking
FOR EACH ROW EXECUTE FUNCTION refresh_booking_slot_capacity();

CREATE TRIGGER trg_refresh_booking_slot_capacity_delete
AFTER DELETE ON booking
FOR EACH ROW EXECUTE FUNCTION refresh_booking_slot_capacity();

-- Payment amount validation
CREATE OR REPLACE FUNCTION validate_payment_paid_amount() RETURNS TRIGGER AS $$
DECLARE
    v_final_amount DECIMAL(10,2);
BEGIN
    IF NEW.status = 'PAID' THEN
        SELECT final_amount INTO v_final_amount FROM booking WHERE booking_id = NEW.booking_id;
        IF v_final_amount IS NULL THEN RAISE EXCEPTION 'Booking not found for payment %', NEW.payment_id; END IF;
        IF NEW.amount <> v_final_amount THEN RAISE EXCEPTION 'Payment amount % does not match booking final amount %', NEW.amount, v_final_amount; END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validate_payment_paid_amount
BEFORE INSERT OR UPDATE OF status, amount, booking_id ON payment
FOR EACH ROW EXECUTE FUNCTION validate_payment_paid_amount();
