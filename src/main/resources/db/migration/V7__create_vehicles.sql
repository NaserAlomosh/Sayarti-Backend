CREATE TABLE vehicles (
 id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
 user_id UNIQUEIDENTIFIER NOT NULL,
 brand NVARCHAR(100) NOT NULL,
 model NVARCHAR(100) NOT NULL,
 vehicle_year INT NOT NULL,
 powertrain_type VARCHAR(30) NOT NULL,
 current_mileage BIGINT NOT NULL,
 license_plate NVARCHAR(30) NULL,
 nickname NVARCHAR(100) NULL,
 image_url NVARCHAR(2048) NULL,
 fuel_type VARCHAR(20) NULL,
 fuel_tank_capacity_liters DECIMAL(10,2) NULL,
 battery_capacity_kwh DECIMAL(10,2) NULL,
 estimated_range_km DECIMAL(10,2) NULL,
 created_at DATETIMEOFFSET(6) NOT NULL,
 updated_at DATETIMEOFFSET(6) NOT NULL,
 deleted_at DATETIMEOFFSET(6) NULL,
 CONSTRAINT fk_vehicles_user FOREIGN KEY (user_id) REFERENCES users(id),
 CONSTRAINT ck_vehicles_year CHECK (vehicle_year >= 1886),
 CONSTRAINT ck_vehicles_powertrain CHECK (powertrain_type IN
   ('GASOLINE','DIESEL','HYBRID','PLUG_IN_HYBRID','ELECTRIC')),
 CONSTRAINT ck_vehicles_fuel_type CHECK (fuel_type IS NULL OR fuel_type IN
   ('GASOLINE_90','GASOLINE_95','GASOLINE_98','DIESEL','OTHER')),
 CONSTRAINT ck_vehicles_mileage CHECK (current_mileage >= 0),
 CONSTRAINT ck_vehicles_fuel_capacity CHECK
   (fuel_tank_capacity_liters IS NULL OR fuel_tank_capacity_liters > 0),
 CONSTRAINT ck_vehicles_battery_capacity CHECK
   (battery_capacity_kwh IS NULL OR battery_capacity_kwh > 0),
 CONSTRAINT ck_vehicles_estimated_range CHECK
   (estimated_range_km IS NULL OR estimated_range_km > 0)
);

CREATE INDEX ix_vehicles_user_deleted ON vehicles(user_id, deleted_at);
CREATE INDEX ix_vehicles_deleted_at ON vehicles(deleted_at);
