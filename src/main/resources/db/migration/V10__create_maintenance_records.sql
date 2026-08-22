CREATE TABLE maintenance_records (
 id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
 vehicle_id UNIQUEIDENTIFIER NOT NULL,
 category VARCHAR(30) NOT NULL,
 title NVARCHAR(200) NOT NULL,
 service_date DATETIMEOFFSET(6) NOT NULL,
 mileage_km DECIMAL(19,2) NOT NULL,
 cost DECIMAL(19,4) NOT NULL,
 currency_code VARCHAR(3) NOT NULL,
 service_provider NVARCHAR(200) NULL,
 notes NVARCHAR(2000) NULL,
 created_at DATETIMEOFFSET(6) NOT NULL,
 updated_at DATETIMEOFFSET(6) NOT NULL,
 deleted_at DATETIMEOFFSET(6) NULL,
 CONSTRAINT fk_maintenance_records_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id),
 CONSTRAINT fk_maintenance_records_currency FOREIGN KEY (currency_code) REFERENCES currencies(code),
 CONSTRAINT ck_maintenance_records_category CHECK (category IN
   ('OIL_CHANGE','FILTER_CHANGE','TIRE_SERVICE','BRAKE_SERVICE','BATTERY','ENGINE',
    'TRANSMISSION','COOLING_SYSTEM','ELECTRICAL','SUSPENSION','AIR_CONDITIONING',
    'INSPECTION','GENERAL_SERVICE','OTHER')),
 CONSTRAINT ck_maintenance_records_mileage CHECK (mileage_km >= 0),
 CONSTRAINT ck_maintenance_records_cost CHECK (cost >= 0),
 CONSTRAINT ck_maintenance_records_title CHECK (LEN(LTRIM(RTRIM(title))) > 0)
);
CREATE INDEX ix_maintenance_records_vehicle_deleted
 ON maintenance_records(vehicle_id, deleted_at);
CREATE INDEX ix_maintenance_records_vehicle_service_date
 ON maintenance_records(vehicle_id, service_date DESC, created_at DESC, id DESC);
CREATE INDEX ix_maintenance_records_deleted_at ON maintenance_records(deleted_at);
