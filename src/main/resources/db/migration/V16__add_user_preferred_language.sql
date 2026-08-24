ALTER TABLE users ADD preferred_language VARCHAR(2) NOT NULL
    CONSTRAINT df_users_preferred_language DEFAULT 'en';

-- SQL Server compiles all statements in a batch before executing them. Start a new batch so the
-- check constraint can resolve the column added above. Flyway's SQL Server parser handles GO as a
-- batch separator and does not send it to the database as SQL.
GO

ALTER TABLE users ADD CONSTRAINT ck_users_preferred_language
    CHECK (preferred_language IN ('en', 'ar'));
