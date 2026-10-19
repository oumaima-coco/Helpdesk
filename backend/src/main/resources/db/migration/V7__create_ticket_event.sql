CREATE TABLE ticket_event (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ticket_id   BIGINT      NOT NULL REFERENCES ticket (id),
    actor_id    BIGINT      REFERENCES app_user (id),
    type        VARCHAR(30) NOT NULL,
    from_status VARCHAR(20),
    to_status   VARCHAR(20) NOT NULL,
    detail      TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_ticket_event_ticket ON ticket_event (ticket_id, created_at);