ALTER TABLE bill_lines ADD COLUMN menu_item_name varchar(200);

UPDATE bill_lines bl
SET menu_item_name = mi.name
    FROM menu_items mi
WHERE bl.menu_item_id = mi.id::text
  AND bl.menu_item_name IS NULL;

UPDATE bill_lines
SET menu_item_name = 'unknown'
WHERE menu_item_name IS NULL;

ALTER TABLE bill_lines ALTER COLUMN menu_item_name SET NOT NULL;
