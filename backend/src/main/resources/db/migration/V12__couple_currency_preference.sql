ALTER TABLE app_users ADD COLUMN currency_code VARCHAR(3) NOT NULL DEFAULT 'USD';

UPDATE app_users
SET currency_code = 'EUR'
WHERE EXISTS (
    SELECT 1
    FROM checklist_items
    WHERE checklist_items.customer_id = app_users.id
      AND checklist_items.budget_amount IS NOT NULL
);
