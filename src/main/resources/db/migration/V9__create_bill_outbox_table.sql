CREATE TABLE bill_outbox_events (
    id UUID PRIMARY KEY,
    bill_id UUID NOT NULL,
    type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    processed BOOLEAN NOT NULL DEFAULT FALSE
    );

CREATE INDEX idx_outbox_billid ON bill_outbox_events(bill_id);
CREATE INDEX idx_outbox_bill_type_created ON bill_outbox_events(type, created_at);
