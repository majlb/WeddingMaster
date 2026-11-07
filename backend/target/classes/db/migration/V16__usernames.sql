ALTER TABLE app_users ADD COLUMN username VARCHAR(30);

UPDATE app_users
SET username = 'user' || id;

ALTER TABLE app_users ALTER COLUMN username SET NOT NULL;
CREATE UNIQUE INDEX uq_app_users_username_lower ON app_users (LOWER(username));
