INSERT INTO bills (
    id,
    table_number,
    bill_status,
    user_id,
    total_amount,
    opened_at,
    closed_at,
    updated_at,
    version
) VALUES (
             '00000000-0000-0000-0000-000000000001',
             5,
             'OPEN',
             'W123',
             72.00,
             '2025-01-01 10:00:00',
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
    user_id,
    total_amount,
    opened_at,
    closed_at,
    updated_at,
    version
) VALUES (
             '00000000-0000-0000-0000-000000000002',
             7,
             'CLOSED',
             'W456',
             50.00,
             '2025-01-01 11:00:00',
             '2025-01-01 11:30:00',
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
