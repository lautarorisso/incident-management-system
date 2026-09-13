-- V7: Poison-pill handling for the outbox poller.
-- Tracks per-event retry attempts and the last failure reason so events that
-- repeatedly fail to publish stop being selected (DB-side DLQ: they remain
-- unpublished and inspectable instead of blocking the batch forever).

ALTER TABLE outbox_events
    ADD COLUMN IF NOT EXISTS attempts   INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS last_error TEXT;