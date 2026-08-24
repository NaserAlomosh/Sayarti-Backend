ALTER TABLE users ADD preferred_language VARCHAR(2) NOT NULL
    CONSTRAINT df_users_preferred_language DEFAULT 'en';
ALTER TABLE users ADD CONSTRAINT ck_users_preferred_language
    CHECK (preferred_language IN ('en', 'ar'));
