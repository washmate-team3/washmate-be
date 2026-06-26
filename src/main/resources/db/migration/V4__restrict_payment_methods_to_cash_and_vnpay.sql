-- WashMate currently supports only two payment methods:
-- CASH for direct payment at the garage and VNPAY for online payment.
UPDATE payment
SET method = 'CASH'
WHERE method NOT IN ('CASH', 'VNPAY');

DO $$
DECLARE
    constraint_record RECORD;
BEGIN
    FOR constraint_record IN
        SELECT con.conname
        FROM pg_constraint con
        JOIN pg_class rel ON rel.oid = con.conrelid
        JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
        WHERE rel.relname = 'payment'
          AND nsp.nspname = 'public'
          AND con.contype = 'c'
          AND pg_get_constraintdef(con.oid) LIKE '%method%'
    LOOP
        EXECUTE format('ALTER TABLE payment DROP CONSTRAINT %I', constraint_record.conname);
    END LOOP;
END
$$;

ALTER TABLE payment
ADD CONSTRAINT chk_payment_method_cash_vnpay
CHECK (method IN ('CASH', 'VNPAY'));
