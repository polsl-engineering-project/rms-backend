INSERT INTO bills (
    id,
    table_number,
    bill_status,
    payment_method,
    waiter_first_name,
    waiter_last_name,
    waiter_employee_id,
    total_amount,
    paid_amount,
    opened_at,
    closed_at,
    paid_at,
    updated_at,
    version
) VALUES (
             '00000000-0000-0000-0000-000000000001',
             5,
             'OPEN',
             NULL,
             'John',
             'Doe',
             'W123',
             72.00,
             0.00,
             '2025-01-01 10:00:00',
             NULL,
             NULL,
             '2025-01-01 10:00:00',
             0
         );

INSERT INTO bill_lines (
    id,
    bill_id,
    menu_item_id,
    quantity,
    unit_price,
    menu_item_name,
    menu_item_version
) VALUES
      ('10000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'pizza', 2, 30.00, 'Pizza', 1),
      ('10000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', 'salad', 1, 12.00, 'Salad', 1);

INSERT INTO bills (
    id,
    table_number,
    bill_status,
    payment_method,
    waiter_first_name,
    waiter_last_name,
    waiter_employee_id,
    total_amount,
    paid_amount,
    opened_at,
    closed_at,
    paid_at,
    updated_at,
    version
) VALUES (
             '00000000-0000-0000-0000-000000000002',
             7,
             'CLOSED',
             NULL,
             'Jane',
             'Smith',
             'W456',
             50.00,
             0.00,
             '2025-01-01 11:00:00',
             '2025-01-01 11:30:00',
             NULL,
             '2025-01-01 11:30:00',
             0
         );

INSERT INTO bill_lines (
    id,
    bill_id,
    menu_item_id,
    quantity,
    unit_price,
    menu_item_name,
    menu_item_version
) VALUES
    ('10000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000002', 'burger', 2, 25.00, 'Burger', 1);

INSERT INTO bills (
    id,
    table_number,
    bill_status,
    payment_method,
    waiter_first_name,
    waiter_last_name,
    waiter_employee_id,
    total_amount,
    paid_amount,
    opened_at,
    closed_at,
    paid_at,
    updated_at,
    version
) VALUES (
             '00000000-0000-0000-0000-000000000003',
             10,
             'PAID',
             'CASH',
             'Alice',
             'Johnson',
             'W789',
             45.50,
             50.00,
             '2025-01-01 09:00:00',
             '2025-01-01 09:45:00',
             '2025-01-01 10:00:00',
             '2025-01-01 10:00:00',
             0
         );


INSERT INTO bill_lines (
    id,
    bill_id,
    menu_item_id,
    quantity,
    unit_price,
    menu_item_name,
    menu_item_version
) VALUES
      ('10000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000003', 'pasta', 1, 25.50, 'Pasta', 1),
      ('10000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000003', 'coffee', 2, 10.00, 'Coffee', 1);