CREATE TABLE reminders (
 id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
 vehicle_id UNIQUEIDENTIFIER NOT NULL,
 category VARCHAR(30) NOT NULL,
 title NVARCHAR(200) NOT NULL,
 description NVARCHAR(2000) NULL,
 trigger_type VARCHAR(10) NOT NULL,
 target_date DATETIMEOFFSET(6) NULL,
 target_mileage BIGINT NULL,
 completed BIT NOT NULL CONSTRAINT df_reminders_completed DEFAULT 0,
 completed_at DATETIMEOFFSET(6) NULL,
 created_at DATETIMEOFFSET(6) NOT NULL,
 updated_at DATETIMEOFFSET(6) NOT NULL,
 deleted_at DATETIMEOFFSET(6) NULL,
 CONSTRAINT fk_reminders_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id),
 CONSTRAINT ck_reminders_category CHECK (category IN
   ('LICENSE_EXPIRATION','INSURANCE_EXPIRATION','MAINTENANCE','OIL_CHANGE','TIRE_SERVICE',
    'REGISTRATION','CUSTOM')),
 CONSTRAINT ck_reminders_trigger_type CHECK (trigger_type IN ('DATE','MILEAGE')),
 CONSTRAINT ck_reminders_title CHECK (LEN(LTRIM(RTRIM(title))) > 0),
 CONSTRAINT ck_reminders_target CHECK (
   (trigger_type = 'DATE' AND target_date IS NOT NULL AND target_mileage IS NULL) OR
   (trigger_type = 'MILEAGE' AND target_date IS NULL AND target_mileage IS NOT NULL
    AND target_mileage >= 0)),
 CONSTRAINT ck_reminders_completion CHECK (
   (completed = 0 AND completed_at IS NULL) OR (completed = 1 AND completed_at IS NOT NULL))
);
CREATE INDEX ix_reminders_vehicle_id ON reminders(vehicle_id);
CREATE INDEX ix_reminders_target_date ON reminders(target_date);
CREATE INDEX ix_reminders_target_mileage ON reminders(target_mileage);
CREATE INDEX ix_reminders_deleted_at ON reminders(deleted_at);
CREATE INDEX ix_reminders_vehicle_history
 ON reminders(vehicle_id, deleted_at, created_at DESC, id DESC);
CREATE INDEX ix_reminders_due_date
 ON reminders(trigger_type, completed, deleted_at, target_date) INCLUDE (vehicle_id);
CREATE INDEX ix_reminders_due_mileage
 ON reminders(trigger_type, completed, deleted_at, target_mileage) INCLUDE (vehicle_id);
