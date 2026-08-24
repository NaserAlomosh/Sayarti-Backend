ALTER TABLE users
    ADD CONSTRAINT ck_users_preferred_language
        CHECK (preferred_language IN ('en', 'ar'));