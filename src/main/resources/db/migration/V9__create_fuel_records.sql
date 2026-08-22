CREATE TABLE fuel_records (
 id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
 vehicle_id UNIQUEIDENTIFIER NOT NULL,
 odometer_km DECIMAL(19,2) NOT NULL,
 quantity_liters DECIMAL(12,3) NOT NULL,
 price_per_liter DECIMAL(19,4) NOT NULL,
 total_cost DECIMAL(19,4) NOT NULL,
 currency_code VARCHAR(3) NOT NULL,
 filled_at DATETIMEOFFSET(6) NOT NULL,
 full_tank BIT NOT NULL,
 station_name NVARCHAR(200) NULL,
 notes NVARCHAR(2000) NULL,
 created_at DATETIMEOFFSET(6) NOT NULL,
 updated_at DATETIMEOFFSET(6) NOT NULL,
 deleted_at DATETIMEOFFSET(6) NULL,
 CONSTRAINT fk_fuel_records_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id),
 CONSTRAINT fk_fuel_records_currency FOREIGN KEY (currency_code) REFERENCES currencies(code),
 CONSTRAINT ck_fuel_records_odometer CHECK (odometer_km >= 0),
 CONSTRAINT ck_fuel_records_quantity CHECK (quantity_liters > 0),
 CONSTRAINT ck_fuel_records_price CHECK (price_per_liter > 0),
 CONSTRAINT ck_fuel_records_total_cost CHECK (total_cost >= 0)
);
CREATE INDEX ix_fuel_records_vehicle_deleted ON fuel_records(vehicle_id, deleted_at);
CREATE INDEX ix_fuel_records_vehicle_filled ON fuel_records(vehicle_id, filled_at);
CREATE INDEX ix_fuel_records_deleted_at ON fuel_records(deleted_at);
