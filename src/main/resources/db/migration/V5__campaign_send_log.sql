-- Nhật ký gửi chiến dịch từ insight (audit + chống gửi trùng).
CREATE TABLE IF NOT EXISTS campaign_send_log (
    log_id           SERIAL PRIMARY KEY,
    insight_id       INTEGER NOT NULL,
    rule_code        VARCHAR(100) NOT NULL,
    garage_id        INTEGER,
    voucher_code     VARCHAR(50),
    sent_count       INTEGER NOT NULL DEFAULT 0,
    failed_count     INTEGER NOT NULL DEFAULT 0,
    sent_by_user_id  INTEGER,
    sent_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_campaign_send_log_insight_sent_at
    ON campaign_send_log(insight_id, sent_at);
