-- Them cot confirmed_by_user_id vao payment_transaction de phuc vu doi soat:
-- ghi lai nhan vien/garage da xac nhan (thu tien) giao dich, dac biet voi thanh toan CASH tai garage.
-- Null voi giao dich tu dong (vi du IPN cua VNPAY).

ALTER TABLE payment_transaction
    ADD COLUMN IF NOT EXISTS confirmed_by_user_id INT;

ALTER TABLE payment_transaction
    DROP CONSTRAINT IF EXISTS fk_payment_transaction_confirmed_by;

ALTER TABLE payment_transaction
    ADD CONSTRAINT fk_payment_transaction_confirmed_by
    FOREIGN KEY (confirmed_by_user_id) REFERENCES app_user(user_id);

CREATE INDEX IF NOT EXISTS idx_payment_txn_confirmed_by
    ON payment_transaction(confirmed_by_user_id)
    WHERE confirmed_by_user_id IS NOT NULL;
