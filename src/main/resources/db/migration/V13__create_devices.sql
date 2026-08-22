CREATE TABLE devices (
 id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
 user_id UNIQUEIDENTIFIER NOT NULL,
 device_identifier VARCHAR(255) NOT NULL,
 platform VARCHAR(10) NOT NULL,
 fcm_token VARCHAR(512) NOT NULL,
 created_at DATETIMEOFFSET(6) NOT NULL,
 updated_at DATETIMEOFFSET(6) NOT NULL,
 CONSTRAINT fk_devices_user FOREIGN KEY (user_id) REFERENCES users(id),
 CONSTRAINT uq_devices_user_identifier UNIQUE (user_id, device_identifier),
 CONSTRAINT uq_devices_fcm_token UNIQUE (fcm_token),
 CONSTRAINT ck_devices_identifier CHECK (LEN(LTRIM(RTRIM(device_identifier))) > 0),
 CONSTRAINT ck_devices_platform CHECK (platform IN ('ANDROID', 'IOS')),
 CONSTRAINT ck_devices_fcm_token CHECK (LEN(LTRIM(RTRIM(fcm_token))) > 0)
);
CREATE INDEX ix_devices_user_id ON devices(user_id);
CREATE INDEX ix_devices_fcm_token ON devices(fcm_token);
CREATE INDEX ix_devices_logical_identity ON devices(user_id, device_identifier);
