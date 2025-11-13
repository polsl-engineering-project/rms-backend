-- sql
BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO users (id, username, password, first_name, last_name, phone_number, role, created_at, updated_at)
VALUES
    (gen_random_uuid(), 'admin',   crypt('AdminPass123!', gen_salt('bf', 12)), 'Admin',   'User',  '+48123456789', 'ADMIN',   now(), now()),
    (gen_random_uuid(), 'manager', crypt('ManagerPass123!', gen_salt('bf', 12)), 'Manager', 'User',  '+48123456788', 'MANAGER', now(), now()),
    (gen_random_uuid(), 'waiter',  crypt('WaiterPass123!', gen_salt('bf', 12)), 'Jan',     'Kowalski', NULL,            'WAITER',  now(), now()),
    (gen_random_uuid(), 'cook',    crypt('CookPass123!', gen_salt('bf', 12)),    'Anna',    'Nowak',  '+48123456787', 'COOK',    now(), now());

COMMIT;