-- Flyway migration: create outbox table for Order events
CREATE TABLE IF NOT EXISTS order_outbox_events (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    processed BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_outbox_orderid ON order_outbox_events(order_id);
CREATE INDEX IF NOT EXISTS idx_outbox_type_created ON order_outbox_events(type, created_at);
