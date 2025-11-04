ALTER TABLE orders ADD COLUMN customer_visible_status VARCHAR(50);

UPDATE orders SET customer_visible_status =
    CASE
        WHEN order_status = 'PENDING_APPROVAL' THEN 'PENDING_APPROVAL'
        WHEN order_status = 'APPROVED_BY_FRONT_DESK' THEN 'PENDING_APPROVAL'
        WHEN order_status = 'CONFIRMED' THEN 'IN_PREPARATION'
        WHEN order_status = 'READY_FOR_PICKUP' THEN 'READY_FOR_PICKUP'
        WHEN order_status = 'READY_FOR_DRIVER' THEN 'IN_PREPARATION'
        WHEN order_status = 'IN_DELIVERY' THEN 'IN_DELIVERY'
        WHEN order_status = 'COMPLETED' THEN 'COMPLETED'
        WHEN order_status = 'CANCELLED' THEN 'CANCELLED'
        ELSE 'PENDING_APPROVAL'
    END;

ALTER TABLE orders ALTER COLUMN customer_visible_status SET ;

