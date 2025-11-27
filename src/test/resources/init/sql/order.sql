-- Init script for integration tests: only data inserts (no DDL)

-- A single pickup order used in tests
INSERT INTO orders (id, order_type, delivery_mode, order_status, delivery_street, delivery_house_number, delivery_apartment_number, delivery_city, delivery_postal_code, customer_first_name, customer_last_name, customer_phone_number, scheduled_for, estimated_preparation_minutes, cancellation_reason, placed_at, updated_at, version)
VALUES
('00000000-0000-0000-0000-000000000001', 'PICKUP', 'SCHEDULED', 'APPROVED', NULL, NULL, NULL, NULL, NULL, 'John', 'Doe', '123456789', '13:00:00', 15, NULL, now(), now(), 0)
ON CONFLICT (id) DO NOTHING;

-- Order lines for the above order
INSERT INTO order_lines (id, order_id, menu_item_id, menu_item_name, quantity, unit_price)
VALUES
('10000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'pizza', 'Pizza', 1, 30.00),
('10000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', 'cake', 'Cake', 1, 15.00)
ON CONFLICT (id) DO NOTHING;

-- Additional sample orders that tests can rely on (if needed)
INSERT INTO orders (id, order_type, delivery_mode, order_status, delivery_street, delivery_house_number, delivery_apartment_number, delivery_city, delivery_postal_code, customer_first_name, customer_last_name, customer_phone_number, scheduled_for, estimated_preparation_minutes, cancellation_reason, placed_at, updated_at, version)
VALUES
('00000000-0000-0000-0000-000000000002', 'DELIVERY', 'SCHEDULED', 'APPROVED', 'Main', '1', NULL, 'City', '00-000', 'Jane', 'Doe', '987654321', '13:00:00', 15, NULL, now(), now(), 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO order_lines (id, order_id, menu_item_id, menu_item_name, quantity, unit_price)
VALUES
('10000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000002', 'wrap', 'Wrap', 1, 18.00)
ON CONFLICT (id) DO NOTHING;

-- Completed order (should be excluded by findActiveOrders)
INSERT INTO orders (id, order_type, delivery_mode, order_status, delivery_street, delivery_house_number, delivery_apartment_number, delivery_city, delivery_postal_code, customer_first_name, customer_last_name, customer_phone_number, scheduled_for, estimated_preparation_minutes, cancellation_reason, placed_at, updated_at, version)
VALUES
('00000000-0000-0000-0000-000000000004', 'DELIVERY', 'ASAP', 'COMPLETED', 'Oak', '4', NULL, 'Town', '11-111', 'Bob', 'Builder', '555000004', NULL, NULL, NULL, now(), now(), 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO order_lines (id, order_id, menu_item_id, menu_item_name, quantity, unit_price)
VALUES
('10000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000004', 'burger', 'Burger', 1, 11.00)
ON CONFLICT (id) DO NOTHING;

-- Cancelled order (should be excluded by findActiveOrders)
INSERT INTO orders (id, order_type, delivery_mode, order_status, delivery_street, delivery_house_number, delivery_apartment_number, delivery_city, delivery_postal_code, customer_first_name, customer_last_name, customer_phone_number, scheduled_for, estimated_preparation_minutes, cancellation_reason, placed_at, updated_at, version)
VALUES
('00000000-0000-0000-0000-000000000005', 'PICKUP', 'ASAP', 'CANCELLED', NULL, NULL, NULL, NULL, NULL, 'Eve', 'Adams', '555000005', NULL, NULL, 'Customer cancelled', now(), now(), 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO order_lines (id, order_id, menu_item_id, menu_item_name, quantity, unit_price)
VALUES
('10000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000005', 'fries', 'Fries', 1, 5.00)
ON CONFLICT (id) DO NOTHING;

-- Another active order (should be included by findActiveOrders)
INSERT INTO orders (id, order_type, delivery_mode, order_status, delivery_street, delivery_house_number, delivery_apartment_number, delivery_city, delivery_postal_code, customer_first_name, customer_last_name, customer_phone_number, scheduled_for, estimated_preparation_minutes, cancellation_reason, placed_at, updated_at, version)
VALUES
('00000000-0000-0000-0000-000000000006', 'PICKUP', 'ASAP', 'PENDING_APPROVAL', NULL, NULL, NULL, NULL, NULL, 'Sam', 'Green', '555000006', NULL, NULL, NULL, now(), now(), 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO order_lines (id, order_id, menu_item_id, menu_item_name, quantity, unit_price)
VALUES
('10000000-0000-0000-0000-000000000006', '00000000-0000-0000-0000-000000000006', 'sandwich', 'Sandwich', 2, 8.50)
ON CONFLICT (id) DO NOTHING;
