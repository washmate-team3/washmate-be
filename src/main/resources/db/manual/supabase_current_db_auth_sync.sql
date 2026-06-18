-- ============================================================================
-- WASHMATE / AUTO WASH PRO - AUTH SYNC FOR CURRENT SUPABASE RESET DB
-- Run this after the current "FINAL RESET SQL - UPGRADED" script.
--
-- Purpose:
-- 1. Add the auth tables required by the backend:
--    - otp_code
--    - refresh_token
-- 2. Sync app_user with production auth flow:
--    - PENDING_VERIFY status
--    - login lock fields
-- 3. Remove DB-side workflow triggers that are now owned by Spring services.
--
-- This script is idempotent and does not drop business data.
-- ============================================================================

-- ============================================================================
-- 1. app_user auth state
-- ============================================================================

DO $$
DECLARE
    constraint_name TEXT;
BEGIN
    SELECT con.conname
    INTO constraint_name
    FROM pg_constraint con
    JOIN pg_class rel ON rel.oid = con.conrelid
    JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
    WHERE rel.relname = 'app_user'
      AND nsp.nspname = current_schema()
      AND con.contype = 'c'
      AND pg_get_constraintdef(con.oid) LIKE '%status%'
    LIMIT 1;

    IF constraint_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE app_user DROP CONSTRAINT %I', constraint_name);
    END IF;
END $$;

ALTER TABLE app_user
    ADD CONSTRAINT chk_app_user_status
    CHECK (status IN ('PENDING_VERIFY','ACTIVE','INACTIVE','BLOCKED','DELETED'));

ALTER TABLE app_user
    ADD COLUMN IF NOT EXISTS failed_login_count INT NOT NULL DEFAULT 0;

ALTER TABLE app_user
    ADD COLUMN IF NOT EXISTS locked_until TIMESTAMPTZ;

ALTER TABLE app_user
    ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMPTZ;

CREATE UNIQUE INDEX IF NOT EXISTS uq_app_user_email_lower
ON app_user (LOWER(email))
WHERE email IS NOT NULL;

-- ============================================================================
-- 2. OTP email-first storage
-- ============================================================================

CREATE TABLE IF NOT EXISTS otp_code (
    id SERIAL PRIMARY KEY,
    phone VARCHAR(20),
    channel VARCHAR(20) NOT NULL DEFAULT 'EMAIL',
    identifier VARCHAR(255) NOT NULL,
    code VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    failed_attempts INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_otp_code_channel
        CHECK (channel IN ('EMAIL', 'PHONE'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_otp_code_channel_identifier
ON otp_code(channel, identifier);

CREATE INDEX IF NOT EXISTS idx_otp_code_channel_identifier
ON otp_code(channel, identifier);

-- ============================================================================
-- 3. Refresh token store for rotation and logout revoke
-- ============================================================================

CREATE TABLE IF NOT EXISTS refresh_token (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES app_user(user_id) ON DELETE CASCADE,
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    replaced_by_token_hash VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_refresh_token_user
ON refresh_token(user_id);

CREATE INDEX IF NOT EXISTS idx_refresh_token_expires_at
ON refresh_token(expires_at);

CREATE INDEX IF NOT EXISTS idx_refresh_token_active_user
ON refresh_token(user_id, expires_at)
WHERE revoked_at IS NULL;

-- ============================================================================
-- 4. Remove DB-side business workflow side effects
-- ============================================================================
-- The backend now owns booking/payment/invoice/loyalty side effects.
-- Guard/validation triggers can remain; only side-effect workflow triggers are removed.

DROP TRIGGER IF EXISTS trg_create_payment_for_booking ON booking;
DROP TRIGGER IF EXISTS trg_sync_booking_on_payment_paid ON payment;
DROP TRIGGER IF EXISTS trg_sync_payment_from_transaction_insert ON payment_transaction;
DROP TRIGGER IF EXISTS trg_sync_payment_from_transaction_update ON payment_transaction;
DROP TRIGGER IF EXISTS trg_create_invoice_when_payment_paid ON payment;
DROP TRIGGER IF EXISTS trg_sync_loyalty_on_booking_completed ON booking;
DROP TRIGGER IF EXISTS trg_sync_loyalty_on_payment_paid ON payment;
DROP TRIGGER IF EXISTS trg_sync_loyalty_on_payment_refunded ON payment;

DROP FUNCTION IF EXISTS create_payment_for_booking();
DROP FUNCTION IF EXISTS sync_booking_on_payment_paid();
DROP FUNCTION IF EXISTS sync_payment_from_transaction();
DROP FUNCTION IF EXISTS create_invoice_when_payment_paid();
DROP FUNCTION IF EXISTS sync_loyalty_on_booking_completed();
DROP FUNCTION IF EXISTS sync_loyalty_on_payment_paid();
DROP FUNCTION IF EXISTS sync_loyalty_on_payment_refunded();
DROP FUNCTION IF EXISTS process_loyalty_for_completed_paid_booking(INT);
DROP FUNCTION IF EXISTS rollback_loyalty_for_refunded_booking(INT);

CREATE INDEX IF NOT EXISTS idx_loyalty_txn_booking_type
ON loyalty_transaction(booking_id, transaction_type);

CREATE INDEX IF NOT EXISTS idx_loyalty_txn_source_type
ON loyalty_transaction(source_transaction_id, transaction_type);

-- ============================================================================
-- 5. Verification query
-- ============================================================================

SELECT table_name
FROM information_schema.tables
WHERE table_schema = current_schema()
  AND table_name IN ('otp_code', 'refresh_token')
ORDER BY table_name;
