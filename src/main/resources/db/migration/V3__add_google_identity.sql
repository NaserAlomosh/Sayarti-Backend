ALTER TABLE users ALTER COLUMN password_hash VARCHAR(100) NULL;
ALTER TABLE users ADD google_subject VARCHAR(255) NULL;
CREATE UNIQUE INDEX uq_users_google_subject ON users (google_subject) WHERE google_subject IS NOT NULL;
