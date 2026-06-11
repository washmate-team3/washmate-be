-- Align DB-side payment/invoice guards with the backend service rules.
-- Data-safe: no table drops, only trigger/function replacement.

-- Payment can only become PAID when it still matches the booking final amount.
CREATE OR REPLACE FUNCTION validate_payment_paid_amount()
RETURNS TRIGGER AS $$
DECLARE
    v_final_amount DECIMAL(10,2);
BEGIN
    IF NEW.status = 'PAID' THEN
        SELECT final_amount
        INTO v_final_amount
        FROM booking
        WHERE booking_id = NEW.booking_id;

        IF v_final_amount IS NULL THEN
            RAISE EXCEPTION 'Booking not found for payment %', NEW.payment_id;
        END IF;

        IF NEW.amount <> v_final_amount THEN
            RAISE EXCEPTION 'Payment amount % does not match booking final amount %', NEW.amount, v_final_amount;
        END IF;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_validate_payment_paid_amount ON payment;
CREATE TRIGGER trg_validate_payment_paid_amount
BEFORE INSERT OR UPDATE OF status, amount, booking_id
ON payment
FOR EACH ROW
EXECUTE FUNCTION validate_payment_paid_amount();

-- Keep invoice sync idempotent for both PAID and REFUNDED payment transitions.
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

        IF NOT FOUND THEN
            RAISE EXCEPTION 'Booking not found for paid payment %', NEW.payment_id;
        END IF;

        SELECT COALESCE(SUM(amount), 0)
        INTO v_penalty_total
        FROM penalty_fee
        WHERE booking_id = NEW.booking_id
          AND status IN ('ACTIVE', 'PAID');

        v_invoice_code := 'INV-' || NEW.payment_id || '-' || TO_CHAR(NOW(), 'YYYYMMDDHH24MISSMS');

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
    ELSIF NEW.status = 'REFUNDED' AND OLD.status <> 'REFUNDED' THEN
        UPDATE invoice
        SET status = 'REFUNDED'
        WHERE booking_id = NEW.booking_id;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_create_invoice_when_payment_paid ON payment;
CREATE TRIGGER trg_create_invoice_when_payment_paid
AFTER UPDATE OF status
ON payment
FOR EACH ROW
EXECUTE FUNCTION create_invoice_when_payment_paid();

-- Preserve refund status when payment_transaction is the source of the update.
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
