CREATE TABLE ticket_attachment (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ticket_id         BIGINT       NOT NULL REFERENCES ticket (id),
    uploader_id       BIGINT       NOT NULL REFERENCES app_user (id),
    original_filename VARCHAR(255) NOT NULL,
    content_type      VARCHAR(100) NOT NULL,
    size_bytes        BIGINT       NOT NULL CHECK (size_bytes > 0),
    storage_key       VARCHAR(100) NOT NULL UNIQUE,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_ticket_attachment_ticket ON ticket_attachment (ticket_id);