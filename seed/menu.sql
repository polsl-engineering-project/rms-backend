-- seed/menu.sql
-- Skrypt do seedowania tabel menu_categories i menu_items
-- Idempotentny: można uruchamiać wielokrotnie, nie duplikuje kategorii ani pozycji (conflict on id/name)

DO $$
DECLARE
    starters_id UUID;
    mains_id UUID;
    desserts_id UUID;
    drinks_id UUID;
BEGIN
    -- Kategorie: INSERT ... ON CONFLICT (name) DO UPDATE SET name = menu_categories.name
    -- Dzięki temu RETURNING id zwróci id zarówno dla wstawionej, jak i istniejącej kategorii
    INSERT INTO menu_categories (id, name, description, active, created_at, updated_at, version)
    VALUES ('11111111-1111-1111-1111-000000000001', 'Starters', 'Light starters to begin your meal', TRUE, now(), now(), 0)
    ON CONFLICT (name) DO UPDATE SET name = menu_categories.name
    RETURNING id INTO starters_id;

    INSERT INTO menu_categories (id, name, description, active, created_at, updated_at, version)
    VALUES ('22222222-2222-2222-2222-000000000002', 'Main Courses', 'Hearty main dishes', TRUE, now(), now(), 0)
    ON CONFLICT (name) DO UPDATE SET name = menu_categories.name
    RETURNING id INTO mains_id;

    INSERT INTO menu_categories (id, name, description, active, created_at, updated_at, version)
    VALUES ('33333333-3333-3333-3333-000000000003', 'Desserts', 'Sweet finishes', TRUE, now(), now(), 0)
    ON CONFLICT (name) DO UPDATE SET name = menu_categories.name
    RETURNING id INTO desserts_id;

    INSERT INTO menu_categories (id, name, description, active, created_at, updated_at, version)
    VALUES ('44444444-4444-4444-4444-000000000004', 'Drinks', 'Cold and hot beverages', TRUE, now(), now(), 0)
    ON CONFLICT (name) DO UPDATE SET name = menu_categories.name
    RETURNING id INTO drinks_id;

    -- Pozycje menu: używamy stałych UUIDów i ON CONFLICT (id) DO NOTHING aby uniknąć duplikatów
    INSERT INTO menu_items (id, name, description, price, available, calories, allergens, vegetarian, vegan, gluten_free, spice_level, category_id, created_at, updated_at, version)
    VALUES
    ('aaaaaaaa-aaaa-aaaa-aaaa-0000000000a1', 'Bruschetta', 'Grilled bread with tomato and basil', 8.50, TRUE, 250, 'gluten', TRUE, FALSE, FALSE, 'NONE', starters_id, now(), now(), 0),
    ('aaaaaaaa-aaaa-aaaa-aaaa-0000000000a2', 'Calamari', 'Fried squid with lemon', 12.00, TRUE, 300, 'seafood,gluten', FALSE, FALSE, FALSE, 'MILD', starters_id, now(), now(), 0),

    ('bbbbbbbb-bbbb-bbbb-bbbb-0000000000b1', 'Grilled Salmon', 'Salmon fillet with herbs', 18.50, TRUE, 600, 'fish', FALSE, FALSE, TRUE, 'NONE', mains_id, now(), now(), 0),
    ('bbbbbbbb-bbbb-bbbb-bbbb-0000000000b2', 'Margherita Pizza', 'Classic pizza with tomato and mozzarella', 14.00, TRUE, 800, 'gluten,milk', FALSE, FALSE, FALSE, 'MILD', mains_id, now(), now(), 0),
    ('bbbbbbbb-bbbb-bbbb-bbbb-0000000000b3', 'Vegan Buddha Bowl', 'Quinoa, roasted veg, avocado', 13.50, TRUE, 550, '', TRUE, TRUE, TRUE, 'NONE', mains_id, now(), now(), 0),

    ('cccccccc-cccc-cccc-cccc-0000000000c1', 'Tiramisu', 'Coffee-flavoured Italian dessert', 7.00, TRUE, 450, 'milk,egg,gluten', FALSE, FALSE, FALSE, 'NONE', desserts_id, now(), now(), 0),
    ('cccccccc-cccc-cccc-cccc-0000000000c2', 'Chocolate Lava Cake', 'Warm chocolate cake with molten center', 8.00, TRUE, 520, 'milk,egg,gluten', FALSE, FALSE, FALSE, 'NONE', desserts_id, now(), now(), 0),

    ('dddddddd-dddd-dddd-dddd-0000000000d1', 'Espresso', 'Strong black coffee', 3.00, TRUE, 5, '', TRUE, TRUE, TRUE, 'NONE', drinks_id, now(), now(), 0),
    ('dddddddd-dddd-dddd-dddd-0000000000d2', 'Spicy Mango Smoothie', 'Mango smoothie with a kick', 5.50, TRUE, 250, '', TRUE, FALSE, TRUE, 'MEDIUM', drinks_id, now(), now(), 0)
    ON CONFLICT (id) DO NOTHING;
END
$$;

-- Koniec skryptu seedującego

