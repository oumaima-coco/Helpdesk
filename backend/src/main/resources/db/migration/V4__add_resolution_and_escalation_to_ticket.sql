ALTER TABLE ticket
    ADD COLUMN resolution TEXT,
    ADD COLUMN escalation_reason TEXT;

ALTER TABLE ticket
    ADD CONSTRAINT resolved_ticket_has_resolution
        CHECK (status <> 'RESOLVED' OR resolution IS NOT NULL);