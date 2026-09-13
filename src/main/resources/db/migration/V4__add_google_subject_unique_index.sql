CREATE UNIQUE INDEX uq_users_google_subject
    ON users (google_subject)
    WHERE google_subject IS NOT NULL;