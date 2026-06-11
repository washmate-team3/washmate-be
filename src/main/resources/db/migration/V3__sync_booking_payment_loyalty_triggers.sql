-- Sync the repository schema with the Supabase v4 behavior used by the
-- booking/payment/loyalty core. This migration is data-safe: no table drops.

-- ============================================================================
-- 1. RBAC compatibility
-- ============================================================================

ALTER TABLE role ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT NOW();
ALTER TABLE role ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ;

ALTER TABLE role DROP CONSTRAINT IF EXISTS role_role_name_check;
ALTER TABLE role DROP CONSTRAINT IF EXISTS chk_role_name;
ALTER TABLE role ADD CONSTRAINT chk_role_name
    CHECK (role_name IN ('CUSTOMER', 'STAFF', 'MANAGER', 'ADMIN', 'OWNER'));

INSERT INTO role(role_name, description, status)
VALUES ('MANAGER', 'Manager role', 'ACTIVE')
ON CONFLICT (role_name) DO NOTHING;

-- ============================================================================
-- 2. Tables required by booking/payment/loyalty triggers
-- ============================================================================

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

    CONSTRAINT uq_loyalty_user_garage
        UNIQUE (user_id, garage_id),
    CONSTRAINT uq_loyalty_account_garage
        UNIQUE (account_id, garage_id),
    CONSTRAINT fk_loyalty_tier_garage
        FOREIGN KEY (tier_id, garage_id)
        REFERENCES membership_tier(tier_id, garage_id),
    CONSTRAINT chk_loyalty_points
        CHECK (total_points >= 0 AND available_points >= 0 AND available_points <= total_points),
    CONSTRAINT chk_loyalty_status
        CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

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

    CONSTRAINT fk_loyalty_transaction_source
        FOREIGN KEY (source_transaction_id)
        REFERENCES loyalty_transaction(transaction_id) ON DELETE RESTRICT,
    CONSTRAINT chk_loyalty_transaction_type
        CHECK (transaction_type IN ('EARN', 'REDEEM', 'REFUND', 'EXPIRE', 'ADJUSTMENT', 'ROLLBACK')),
    CONSTRAINT chk_loyalty_transaction_points
        CHECK (points <> 0)
);

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

    CONSTRAINT chk_loyalty_tier_history_type
        CHECK (change_type IN ('UPGRADE', 'DOWNGRADE', 'ADJUSTMENT'))
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

    CONSTRAINT fk_penalty_booking_garage
        FOREIGN KEY (booking_id, garage_id)
        REFERENCES booking(booking_id, garage_id) ON DELETE RESTRICT,
    CONSTRAINT chk_penalty_amount
        CHECK (amount >= 0),
    CONSTRAINT chk_penalty_type
        CHECK (penalty_type IN ('LATE_ARRIVAL', 'NO_SHOW', 'EXTRA_SERVICE', 'DAMAGE', 'OTHER')),
    CONSTRAINT chk_penalty_status
        CHECK (status IN ('ACTIVE', 'WAIVED', 'PAID', 'CANCELLED'))
);

CREATE INDEX IF NOT EXISTS idx_loyalty_account_user_garage
ON loyalty_account(user_id, garage_id);

CREATE INDEX IF NOT EXISTS idx_loyalty_txn_account
ON loyalty_transaction(account_id);

CREATE INDEX IF NOT EXISTS idx_loyalty_txn_booking_earn
ON loyalty_transaction(booking_id)
WHERE transaction_type = 'EARN';

CREATE INDEX IF NOT EXISTS idx_loyalty_txn_source
ON loyalty_transaction(source_transaction_id);

CREATE INDEX IF NOT EXISTS idx_penalty_booking
ON penalty_fee(booking_id);

-- ============================================================================
-- 3. Updated-at helpers
-- ============================================================================

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at := NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_garage_updated_at ON garage;
CREATE TRIGGER trg_garage_updated_at
BEFORE UPDATE ON garage
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_app_user_updated_at ON app_user;
CREATE TRIGGER trg_app_user_updated_at
BEFORE UPDATE ON app_user
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_role_updated_at ON role;
CREATE TRIGGER trg_role_updated_at
BEFORE UPDATE ON role
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_vehicle_updated_at ON vehicle;
CREATE TRIGGER trg_vehicle_updated_at
BEFORE UPDATE ON vehicle
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_service_package_updated_at ON service_package;
CREATE TRIGGER trg_service_package_updated_at
BEFORE UPDATE ON service_package
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_loyalty_policy_updated_at ON loyalty_policy;
CREATE TRIGGER trg_loyalty_policy_updated_at
BEFORE UPDATE ON loyalty_policy
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_loyalty_account_updated_at ON loyalty_account;
CREATE TRIGGER trg_loyalty_account_updated_at
BEFORE UPDATE ON loyalty_account
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_booking_updated_at ON booking;
CREATE TRIGGER trg_booking_updated_at
BEFORE UPDATE ON booking
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_payment_updated_at ON payment;
CREATE TRIGGER trg_payment_updated_at
BEFORE UPDATE ON payment
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ============================================================================
-- 4. Booking guard and slot capacity
-- ============================================================================

CREATE OR REPLACE FUNCTION validate_booking_date_guard()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.status IN ('PENDING', 'CONFIRMED', 'CHECKED_IN', 'WASHING')
       AND NEW.booking_date < CURRENT_DATE THEN
        RAISE EXCEPTION 'booking_date cannot be in the past for active bookings';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_validate_booking_date_guard ON booking;
CREATE TRIGGER trg_validate_booking_date_guard
BEFORE INSERT OR UPDATE OF booking_date, status
ON booking
FOR EACH ROW
EXECUTE FUNCTION validate_booking_date_guard();

CREATE OR REPLACE FUNCTION validate_assigned_staff_role()
RETURNS TRIGGER AS $$
DECLARE
    staff_role_count INT;
BEGIN
    IF NEW.assigned_staff_user_id IS NULL THEN
        RETURN NEW;
    END IF;

    SELECT COUNT(*)
    INTO staff_role_count
    FROM user_role ur
    JOIN role r ON ur.role_id = r.role_id
    JOIN app_user au ON au.user_id = ur.user_id
    WHERE ur.user_id = NEW.assigned_staff_user_id
      AND ur.garage_id = NEW.garage_id
      AND ur.status = 'ACTIVE'
      AND r.role_name = 'STAFF'
      AND r.status = 'ACTIVE'
      AND au.status = 'ACTIVE';

    IF staff_role_count = 0 THEN
        RAISE EXCEPTION 'assigned_staff_user_id must be an active STAFF in the same garage';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_validate_assigned_staff_role ON booking;
CREATE TRIGGER trg_validate_assigned_staff_role
BEFORE INSERT OR UPDATE OF assigned_staff_user_id, garage_id
ON booking
FOR EACH ROW
EXECUTE FUNCTION validate_assigned_staff_role();

CREATE OR REPLACE FUNCTION check_booking_slot_capacity()
RETURNS TRIGGER AS $$
DECLARE
    v_max_capacity INT;
    v_current_count INT;
BEGIN
    IF NEW.status NOT IN ('PENDING', 'CONFIRMED', 'CHECKED_IN', 'WASHING') THEN
        RETURN NEW;
    END IF;

    SELECT max_capacity
    INTO v_max_capacity
    FROM booking_slot
    WHERE slot_id = NEW.slot_id
      AND garage_id = NEW.garage_id
      AND status = 'ACTIVE'
    FOR UPDATE;

    IF v_max_capacity IS NULL THEN
        RAISE EXCEPTION 'Booking slot does not exist or is not active for this garage';
    END IF;

    SELECT COUNT(*)
    INTO v_current_count
    FROM booking
    WHERE slot_id = NEW.slot_id
      AND garage_id = NEW.garage_id
      AND booking_date = NEW.booking_date
      AND status IN ('PENDING', 'CONFIRMED', 'CHECKED_IN', 'WASHING')
      AND (TG_OP = 'INSERT' OR booking_id <> NEW.booking_id);

    IF v_current_count >= v_max_capacity THEN
        RAISE EXCEPTION 'Selected booking slot is full for this date';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_check_booking_slot_capacity ON booking;
CREATE TRIGGER trg_check_booking_slot_capacity
BEFORE INSERT OR UPDATE OF status, slot_id, garage_id, booking_date
ON booking
FOR EACH ROW
EXECUTE FUNCTION check_booking_slot_capacity();

CREATE OR REPLACE FUNCTION refresh_one_slot_capacity(
    p_slot_id INT,
    p_garage_id INT,
    p_booking_date DATE
)
RETURNS VOID AS $$
DECLARE
    v_current_count INT;
BEGIN
    IF p_slot_id IS NULL OR p_garage_id IS NULL OR p_booking_date IS NULL THEN
        RETURN;
    END IF;

    SELECT COUNT(*)
    INTO v_current_count
    FROM booking
    WHERE slot_id = p_slot_id
      AND garage_id = p_garage_id
      AND booking_date = p_booking_date
      AND status IN ('PENDING', 'CONFIRMED', 'CHECKED_IN', 'WASHING');

    INSERT INTO booking_slot_capacity(slot_id, garage_id, booking_date, current_capacity, updated_at)
    VALUES (p_slot_id, p_garage_id, p_booking_date, v_current_count, NOW())
    ON CONFLICT (slot_id, booking_date)
    DO UPDATE SET
        garage_id = EXCLUDED.garage_id,
        current_capacity = EXCLUDED.current_capacity,
        updated_at = NOW();
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION refresh_booking_slot_capacity()
RETURNS TRIGGER AS $$
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

DROP TRIGGER IF EXISTS trg_refresh_booking_slot_capacity_insert_update ON booking;
CREATE TRIGGER trg_refresh_booking_slot_capacity_insert_update
AFTER INSERT OR UPDATE OF status, slot_id, garage_id, booking_date
ON booking
FOR EACH ROW
EXECUTE FUNCTION refresh_booking_slot_capacity();

DROP TRIGGER IF EXISTS trg_refresh_booking_slot_capacity_delete ON booking;
CREATE TRIGGER trg_refresh_booking_slot_capacity_delete
AFTER DELETE ON booking
FOR EACH ROW
EXECUTE FUNCTION refresh_booking_slot_capacity();

-- ============================================================================
-- 5. Payment, invoice, and booking sync
-- ============================================================================

CREATE OR REPLACE FUNCTION create_payment_for_booking()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO payment(booking_id, garage_id, amount, method, status)
    VALUES (NEW.booking_id, NEW.garage_id, NEW.final_amount, 'CASH', 'PENDING')
    ON CONFLICT (booking_id) DO NOTHING;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_create_payment_for_booking ON booking;
CREATE TRIGGER trg_create_payment_for_booking
AFTER INSERT ON booking
FOR EACH ROW
EXECUTE FUNCTION create_payment_for_booking();

CREATE OR REPLACE FUNCTION sync_booking_on_payment_paid()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.status = 'PAID' AND OLD.status <> 'PAID' THEN
        UPDATE booking
        SET status = CASE WHEN status = 'PENDING' THEN 'CONFIRMED' ELSE status END,
            confirmed_at = COALESCE(confirmed_at, NOW()),
            updated_at = NOW()
        WHERE booking_id = NEW.booking_id;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_sync_booking_on_payment_paid ON payment;
CREATE TRIGGER trg_sync_booking_on_payment_paid
AFTER UPDATE OF status ON payment
FOR EACH ROW
EXECUTE FUNCTION sync_booking_on_payment_paid();

CREATE OR REPLACE FUNCTION sync_payment_from_transaction()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.status = 'SUCCESS' THEN
        UPDATE payment
        SET status = 'PAID',
            paid_at = COALESCE(paid_at, NOW()),
            updated_at = NOW()
        WHERE payment_id = NEW.payment_id
          AND status <> 'PAID';
    ELSIF NEW.status IN ('FAILED', 'CANCELLED') THEN
        UPDATE payment
        SET status = CASE WHEN status = 'PAID' THEN status ELSE NEW.status END,
            updated_at = NOW()
        WHERE payment_id = NEW.payment_id;
    ELSIF NEW.status = 'REFUNDED' THEN
        UPDATE payment
        SET status = 'REFUNDED',
            updated_at = NOW()
        WHERE payment_id = NEW.payment_id;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_sync_payment_from_transaction_insert ON payment_transaction;
CREATE TRIGGER trg_sync_payment_from_transaction_insert
AFTER INSERT ON payment_transaction
FOR EACH ROW
EXECUTE FUNCTION sync_payment_from_transaction();

DROP TRIGGER IF EXISTS trg_sync_payment_from_transaction_update ON payment_transaction;
CREATE TRIGGER trg_sync_payment_from_transaction_update
AFTER UPDATE OF status ON payment_transaction
FOR EACH ROW
EXECUTE FUNCTION sync_payment_from_transaction();

CREATE OR REPLACE FUNCTION create_invoice_when_payment_paid()
RETURNS TRIGGER AS $$
DECLARE
    v_booking booking%ROWTYPE;
    v_penalty_total DECIMAL(10,2);
    v_invoice_code VARCHAR(50);
BEGIN
    IF NEW.status = 'PAID' AND OLD.status <> 'PAID' THEN
        SELECT *
        INTO v_booking
        FROM booking
        WHERE booking_id = NEW.booking_id;

        SELECT COALESCE(SUM(amount), 0)
        INTO v_penalty_total
        FROM penalty_fee
        WHERE booking_id = NEW.booking_id
          AND status IN ('ACTIVE', 'PAID');

        v_invoice_code := 'INV-' || NEW.payment_id || '-' || TO_CHAR(NOW(), 'YYYYMMDDHH24MISS');

        INSERT INTO invoice(
            invoice_code,
            booking_id,
            payment_id,
            garage_id,
            subtotal,
            discount,
            penalty_total,
            total_amount,
            status,
            paid_at
        )
        VALUES (
            v_invoice_code,
            v_booking.booking_id,
            NEW.payment_id,
            v_booking.garage_id,
            v_booking.total_amount,
            v_booking.discount_amount,
            v_penalty_total,
            v_booking.final_amount + v_penalty_total,
            'PAID',
            COALESCE(NEW.paid_at, NOW())
        )
        ON CONFLICT (booking_id)
        DO UPDATE SET
            payment_id = EXCLUDED.payment_id,
            subtotal = EXCLUDED.subtotal,
            discount = EXCLUDED.discount,
            penalty_total = EXCLUDED.penalty_total,
            total_amount = EXCLUDED.total_amount,
            status = 'PAID',
            paid_at = EXCLUDED.paid_at;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_create_invoice_when_payment_paid ON payment;
CREATE TRIGGER trg_create_invoice_when_payment_paid
AFTER UPDATE OF status ON payment
FOR EACH ROW
EXECUTE FUNCTION create_invoice_when_payment_paid();

-- ============================================================================
-- 6. Loyalty earn and rollback
-- ============================================================================

CREATE OR REPLACE FUNCTION process_loyalty_for_completed_paid_booking(p_booking_id INT)
RETURNS VOID AS $$
DECLARE
    v_booking booking%ROWTYPE;
    v_points_earned INT;
    v_account_id INT;
    v_current_tier INT;
    v_new_tier_id INT;
    v_default_tier INT;
    v_amount_per_point DECIMAL(10,2);
    v_expiry_months INT;
    v_auto_enroll BOOLEAN;
    v_current_min_points INT;
    v_new_min_points INT;
    v_change_type VARCHAR(20);
    v_change_reason TEXT;
BEGIN
    SELECT b.*
    INTO v_booking
    FROM booking b
    JOIN payment p ON p.booking_id = b.booking_id
    WHERE b.booking_id = p_booking_id
      AND b.status = 'COMPLETED'
      AND p.status = 'PAID';

    IF NOT FOUND THEN
        RETURN;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM loyalty_transaction
        WHERE booking_id = v_booking.booking_id
          AND transaction_type = 'EARN'
    ) THEN
        RETURN;
    END IF;

    SELECT amount_per_point, point_expiry_months, auto_enroll
    INTO v_amount_per_point, v_expiry_months, v_auto_enroll
    FROM loyalty_policy
    WHERE garage_id = v_booking.garage_id
      AND status = 'ACTIVE';

    IF v_amount_per_point IS NULL THEN
        v_amount_per_point := 10000;
        v_expiry_months := 12;
        v_auto_enroll := TRUE;
    END IF;

    v_points_earned := FLOOR(v_booking.final_amount / v_amount_per_point);

    IF v_points_earned <= 0 THEN
        RETURN;
    END IF;

    SELECT account_id, tier_id
    INTO v_account_id, v_current_tier
    FROM loyalty_account
    WHERE user_id = v_booking.user_id
      AND garage_id = v_booking.garage_id
      AND status = 'ACTIVE';

    IF NOT FOUND THEN
        IF v_auto_enroll = FALSE THEN
            RETURN;
        END IF;

        SELECT tier_id
        INTO v_default_tier
        FROM membership_tier
        WHERE garage_id = v_booking.garage_id
          AND status = 'ACTIVE'
        ORDER BY min_points ASC
        LIMIT 1;

        IF v_default_tier IS NULL THEN
            RAISE EXCEPTION 'No default membership tier found for garage %', v_booking.garage_id;
        END IF;

        INSERT INTO loyalty_account(user_id, garage_id, tier_id, total_points, available_points)
        VALUES (v_booking.user_id, v_booking.garage_id, v_default_tier, 0, 0)
        RETURNING account_id, tier_id
        INTO v_account_id, v_current_tier;
    END IF;

    UPDATE loyalty_account
    SET total_points = total_points + v_points_earned,
        available_points = available_points + v_points_earned,
        updated_at = NOW()
    WHERE account_id = v_account_id;

    INSERT INTO loyalty_transaction(
        account_id,
        booking_id,
        points,
        transaction_type,
        description,
        earned_at,
        expires_at
    )
    VALUES (
        v_account_id,
        v_booking.booking_id,
        v_points_earned,
        'EARN',
        'Earned points from completed paid booking',
        NOW(),
        NOW() + (v_expiry_months || ' months')::INTERVAL
    );

    SELECT tier_id
    INTO v_new_tier_id
    FROM membership_tier
    WHERE garage_id = v_booking.garage_id
      AND status = 'ACTIVE'
      AND min_points <= (
          SELECT total_points
          FROM loyalty_account
          WHERE account_id = v_account_id
      )
    ORDER BY min_points DESC
    LIMIT 1;

    IF v_new_tier_id IS NOT NULL AND v_new_tier_id <> v_current_tier THEN
        SELECT min_points
        INTO v_current_min_points
        FROM membership_tier
        WHERE tier_id = v_current_tier
          AND garage_id = v_booking.garage_id;

        SELECT min_points
        INTO v_new_min_points
        FROM membership_tier
        WHERE tier_id = v_new_tier_id
          AND garage_id = v_booking.garage_id;

        IF COALESCE(v_new_min_points, 0) > COALESCE(v_current_min_points, 0) THEN
            v_change_type := 'UPGRADE';
            v_change_reason := 'Auto tier upgrade after earning points';
        ELSIF COALESCE(v_new_min_points, 0) < COALESCE(v_current_min_points, 0) THEN
            v_change_type := 'DOWNGRADE';
            v_change_reason := 'Auto tier downgrade after point adjustment';
        ELSE
            v_change_type := 'ADJUSTMENT';
            v_change_reason := 'Auto tier adjustment';
        END IF;

        UPDATE loyalty_account
        SET tier_id = v_new_tier_id,
            updated_at = NOW()
        WHERE account_id = v_account_id;

        INSERT INTO loyalty_tier_history(
            account_id,
            garage_id,
            old_tier_id,
            new_tier_id,
            changed_by_user_id,
            change_reason,
            change_type
        )
        VALUES (
            v_account_id,
            v_booking.garage_id,
            v_current_tier,
            v_new_tier_id,
            NULL,
            v_change_reason,
            v_change_type
        );
    END IF;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION sync_loyalty_on_booking_completed()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.status = 'COMPLETED' AND OLD.status <> 'COMPLETED' THEN
        PERFORM process_loyalty_for_completed_paid_booking(NEW.booking_id);
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_sync_loyalty_on_booking_completed ON booking;
CREATE TRIGGER trg_sync_loyalty_on_booking_completed
AFTER UPDATE OF status ON booking
FOR EACH ROW
EXECUTE FUNCTION sync_loyalty_on_booking_completed();

CREATE OR REPLACE FUNCTION sync_loyalty_on_payment_paid()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.status = 'PAID' AND OLD.status <> 'PAID' THEN
        PERFORM process_loyalty_for_completed_paid_booking(NEW.booking_id);
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_sync_loyalty_on_payment_paid ON payment;
CREATE TRIGGER trg_sync_loyalty_on_payment_paid
AFTER UPDATE OF status ON payment
FOR EACH ROW
EXECUTE FUNCTION sync_loyalty_on_payment_paid();

CREATE OR REPLACE FUNCTION rollback_loyalty_for_refunded_booking(p_booking_id INT)
RETURNS VOID AS $$
DECLARE
    earned RECORD;
BEGIN
    FOR earned IN
        SELECT transaction_id, account_id, points
        FROM loyalty_transaction
        WHERE booking_id = p_booking_id
          AND transaction_type = 'EARN'
          AND NOT EXISTS (
              SELECT 1
              FROM loyalty_transaction rollback
              WHERE rollback.transaction_type = 'ROLLBACK'
                AND rollback.source_transaction_id = loyalty_transaction.transaction_id
          )
    LOOP
        UPDATE loyalty_account
        SET available_points = GREATEST(available_points - earned.points, 0),
            total_points = GREATEST(total_points - earned.points, 0),
            updated_at = NOW()
        WHERE account_id = earned.account_id;

        INSERT INTO loyalty_transaction(
            account_id,
            booking_id,
            source_transaction_id,
            points,
            transaction_type,
            description,
            earned_at
        )
        VALUES (
            earned.account_id,
            p_booking_id,
            earned.transaction_id,
            -earned.points,
            'ROLLBACK',
            'Rollback earned points after payment refund',
            NOW()
        );
    END LOOP;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION sync_loyalty_on_payment_refunded()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.status = 'REFUNDED' AND OLD.status <> 'REFUNDED' THEN
        PERFORM rollback_loyalty_for_refunded_booking(NEW.booking_id);
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_sync_loyalty_on_payment_refunded ON payment;
CREATE TRIGGER trg_sync_loyalty_on_payment_refunded
AFTER UPDATE OF status ON payment
FOR EACH ROW
EXECUTE FUNCTION sync_loyalty_on_payment_refunded();
