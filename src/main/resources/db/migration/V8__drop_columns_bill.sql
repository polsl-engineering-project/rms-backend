ALTER TABLE bills DROP COLUMN payment_method;
ALTER TABLE bills DROP COLUMN waiter_first_name;
ALTER TABLE bills DROP COLUMN waiter_last_name;
ALTER TABLE bills DROP COLUMN paid_amount;
ALTER TABLE bills DROP COLUMN paid_at;
ALTER TABLE bills RENAME COLUMN waiter_employee_id TO user_id;