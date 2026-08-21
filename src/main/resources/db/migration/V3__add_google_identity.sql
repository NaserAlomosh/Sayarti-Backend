-- Relaxing the password constraint preserves every LOCAL row while allowing GOOGLE-only users.
ALTER TABLE users ALTER COLUMN password_hash VARCHAR(100) NULL;
-- Existing rows remain valid because the new identity column is nullable.
ALTER TABLE users ADD google_subject VARCHAR(255) NULL;
-- SQL Server unique constraints treat NULL as a value; filter NULLs so all LOCAL rows remain valid.
CREATE UNIQUE INDEX uq_users_google_subject ON users (google_subject) WHERE google_subject IS NOT NULL;
