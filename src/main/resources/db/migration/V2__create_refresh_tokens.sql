CREATE TABLE refresh_tokens (
 id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
 user_id UNIQUEIDENTIFIER NOT NULL,
 token_hash VARCHAR(64) NOT NULL,
 expires_at DATETIMEOFFSET(6) NOT NULL,
 created_at DATETIMEOFFSET(6) NOT NULL,
 revoked_at DATETIMEOFFSET(6) NULL,
 replaced_by_token_id UNIQUEIDENTIFIER NULL,
 CONSTRAINT uq_refresh_tokens_hash UNIQUE (token_hash),
 CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id),
 CONSTRAINT fk_refresh_tokens_replacement FOREIGN KEY (replaced_by_token_id) REFERENCES refresh_tokens(id)
);
CREATE INDEX ix_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX ix_refresh_tokens_expires_at ON refresh_tokens(expires_at);
