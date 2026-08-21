CREATE TABLE users (
 id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
 first_name NVARCHAR(100) NOT NULL,
 last_name NVARCHAR(100) NOT NULL,
 email VARCHAR(320) NOT NULL,
 password_hash VARCHAR(100) NOT NULL,
 auth_provider VARCHAR(20) NOT NULL,
 created_at DATETIMEOFFSET(6) NOT NULL,
 updated_at DATETIMEOFFSET(6) NOT NULL,
 deleted_at DATETIMEOFFSET(6) NULL,
 CONSTRAINT uq_users_email UNIQUE (email),
 CONSTRAINT ck_users_auth_provider CHECK (auth_provider IN ('LOCAL','GOOGLE'))
);
