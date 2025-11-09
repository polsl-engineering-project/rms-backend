-- Flyway migration: create outbox table for Order events
CREATE TABLE IF NOT EXISTS order_outbox_event (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT
);

CREATE INDEX IF NOT EXISTS idx_outbox_orderid ON order_outbox_event(order_id);
CREATE INDEX IF NOT EXISTS idx_outbox_type_created ON order_outbox_event(type, created_at);

