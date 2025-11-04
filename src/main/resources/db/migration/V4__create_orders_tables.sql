-- Flyway migration V3: create orders and order_lines tables for PostgreSQL
-- This migration maps the JPA entities Order and OrderLine to SQL tables.

CREATE TABLE orders (
    id UUID NOT NULL,
    order_type VARCHAR(50) NOT NULL,
    delivery_mode VARCHAR(50) NOT NULL,
    order_status VARCHAR(50) NOT NULL,

    -- delivery address (embedded). Nullable for pickup orders
    delivery_street VARCHAR(255),
    delivery_house_number VARCHAR(50),
    delivery_apartment_number VARCHAR(50),
    delivery_city VARCHAR(100),
    delivery_postal_code VARCHAR(32),

    -- customer info (embedded). These fields are required by the Java record
    customer_first_name VARCHAR(100) NOT NULL,
    customer_last_name VARCHAR(100) NOT NULL,
    customer_phone_number VARCHAR(32) NOT NULL,

    scheduled_for TIME,
    estimated_preparation_minutes INTEGER,
    cancellation_reason VARCHAR(1024),

    placed_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,

    -- optimistic locking version
    version BIGINT NOT NULL,

    CONSTRAINT pk_orders PRIMARY KEY (id)
);

-- order lines: each line references orders(id)
CREATE TABLE order_lines (
    id UUID NOT NULL,
    order_id UUID NOT NULL,
    menu_item_id VARCHAR(200) NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price NUMERIC(12,2) NOT NULL,

    CONSTRAINT pk_order_lines PRIMARY KEY (id),
    CONSTRAINT fk_order_lines_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT chk_order_lines_quantity_positive CHECK (quantity > 0),
    CONSTRAINT chk_order_lines_unit_price_non_negative CHECK (unit_price >= 0)
);

CREATE INDEX idx_order_lines_order_id ON order_lines(order_id);
CREATE INDEX idx_orders_order_status ON orders(order_status);
CREATE INDEX idx_orders_placed_at ON orders(placed_at);

-- Notes:
-- - UUID is used because OrderId/OrderLineId are UUID-based records in Java.
-- - Money.amount is mapped to NUMERIC(12,2) (scale 2) to match Money record behavior.
-- - Embedded address/customer fields are expanded to separate columns and are nullable where appropriate.
-- - Add further constraints/indexes as needed by queries in the application.

