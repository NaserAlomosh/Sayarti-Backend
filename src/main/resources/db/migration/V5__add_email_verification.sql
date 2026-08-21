ALTER TABLE users ADD email_verified BIT NOT NULL CONSTRAINT df_users_email_verified DEFAULT 0;
UPDATE users SET email_verified = 1 WHERE auth_provider = 'GOOGLE';

CREATE TABLE email_verification_otps (
 id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
 user_id UNIQUEIDENTIFIER NOT NULL,
 otp_hash VARCHAR(100) NOT NULL,
 expires_at DATETIMEOFFSET(6) NOT NULL,
 attempt_count INT NOT NULL CONSTRAINT df_email_otp_attempt_count DEFAULT 0,
 verified_at DATETIMEOFFSET(6) NULL,
 created_at DATETIMEOFFSET(6) NOT NULL,
 invalidated_at DATETIMEOFFSET(6) NULL,
 CONSTRAINT fk_email_verification_otps_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
 CONSTRAINT ck_email_otp_attempt_count CHECK (attempt_count >= 0)
);
CREATE INDEX ix_email_verification_otps_user_id ON email_verification_otps(user_id);
CREATE INDEX ix_email_verification_otps_expires_at ON email_verification_otps(expires_at);
CREATE UNIQUE INDEX uq_email_verification_otps_active_user ON email_verification_otps(user_id)
 WHERE verified_at IS NULL AND invalidated_at IS NULL;
