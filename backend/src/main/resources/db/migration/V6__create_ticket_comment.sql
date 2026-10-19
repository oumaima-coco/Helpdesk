CREATE TABLE ticket_comment (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ticket_id  BIGINT      NOT NULL REFERENCES ticket (id),
    author_id  BIGINT      NOT NULL REFERENCES app_user (id),
    body       TEXT        NOT NULL,
    internal   BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_ticket_comment_ticket ON ticket_comment (ticket_id, created_at);