ALTER TABLE ticket
    ADD COLUMN pickup_due_at            TIMESTAMPTZ,
    ADD COLUMN resolution_due_at        TIMESTAMPTZ,
    ADD COLUMN paused_remaining_seconds BIGINT CHECK (paused_remaining_seconds >= 0);

CREATE INDEX idx_ticket_pickup_due ON ticket (pickup_due_at) WHERE pickup_due_at IS NOT NULL;
CREATE INDEX idx_ticket_resolution_due ON ticket (resolution_due_at) WHERE resolution_due_at IS NOT NULL;