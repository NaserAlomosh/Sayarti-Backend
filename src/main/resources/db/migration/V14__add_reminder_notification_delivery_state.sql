ALTER TABLE reminders ADD notification_delivered_at DATETIMEOFFSET(6) NULL;

CREATE INDEX ix_reminders_notification_poll
 ON reminders(notification_delivered_at, completed, deleted_at, trigger_type)
 INCLUDE (vehicle_id, target_date, target_mileage);
