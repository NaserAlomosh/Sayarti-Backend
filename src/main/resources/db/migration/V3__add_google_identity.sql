ALTER TABLE users
ALTER COLUMN password_hash VARCHAR(100) NULL;

ALTER TABLE users
    ADD google_subject VARCHAR(255) NULL;