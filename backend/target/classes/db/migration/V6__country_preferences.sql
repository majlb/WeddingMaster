ALTER TABLE app_users ADD COLUMN country_codes JSONB NOT NULL DEFAULT '[]'::jsonb;
ALTER TABLE suppliers ADD COLUMN country_codes JSONB NOT NULL DEFAULT '[]'::jsonb;
