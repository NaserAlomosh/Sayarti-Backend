ALTER TABLE users
    ADD preferred_language VARCHAR(2) NOT NULL
        CONSTRAINT df_users_preferred_language DEFAULT 'en';