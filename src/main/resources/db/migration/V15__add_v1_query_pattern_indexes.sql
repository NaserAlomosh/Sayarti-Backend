-- Vehicle lists are scoped to an owner, exclude soft-deleted rows, and show newest first.
CREATE INDEX ix_vehicles_user_active_created
 ON vehicles(user_id, deleted_at, created_at DESC);

-- Fuel history, fuel statistics, and recent activity all start with the active records of one
-- vehicle. The trailing columns match the deterministic history ordering.
CREATE INDEX ix_fuel_records_vehicle_active_filled
 ON fuel_records(vehicle_id, deleted_at, filled_at DESC, created_at DESC, id DESC);

-- Maintenance history/statistics use the same ownership, soft-delete, and timeline pattern.
CREATE INDEX ix_maintenance_records_vehicle_active_service
 ON maintenance_records(vehicle_id, deleted_at, service_date DESC, created_at DESC, id DESC);

-- Dashboard reminder lookup filters incomplete rows before applying newest-first ordering.
CREATE INDEX ix_reminders_vehicle_active_incomplete
 ON reminders(vehicle_id, deleted_at, completed, created_at DESC, id DESC);

-- Recent activity reads only completed reminders and orders by their completion timestamp.
CREATE INDEX ix_reminders_vehicle_active_completed
 ON reminders(vehicle_id, deleted_at, completed, completed_at DESC, id DESC);

-- The scheduler first selects undelivered, incomplete, active reminders. Separate filtered
-- indexes keep each trigger path narrow while retaining the stable batch ordering and vehicle
-- lookup needed by the due-reminder query. Row locking still occurs through the primary key.
CREATE INDEX ix_reminders_pending_date_trigger
 ON reminders(target_date, created_at, id)
 INCLUDE (vehicle_id)
 WHERE notification_delivered_at IS NULL
   AND completed = 0
   AND deleted_at IS NULL
   AND trigger_type = 'DATE';

CREATE INDEX ix_reminders_pending_mileage_trigger
 ON reminders(target_mileage, created_at, id)
 INCLUDE (vehicle_id)
 WHERE notification_delivered_at IS NULL
   AND completed = 0
   AND deleted_at IS NULL
   AND trigger_type = 'MILEAGE';
