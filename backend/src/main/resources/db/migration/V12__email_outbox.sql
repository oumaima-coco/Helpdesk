CREATE TABLE email_outbox (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    recipient  VARCHAR(255) NOT NULL,
    subject    VARCHAR(255) NOT NULL,
    body       TEXT         NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    sent_at    TIMESTAMPTZ,
    attempts   INT          NOT NULL DEFAULT 0,
    last_error TEXT
);

CREATE INDEX idx_email_outbox_pending ON email_outbox (id) WHERE sent_at IS NULL;