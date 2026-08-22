CREATE TABLE expenses (
 id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
 vehicle_id UNIQUEIDENTIFIER NOT NULL,
 category VARCHAR(20) NOT NULL,
 title NVARCHAR(200) NOT NULL,
 expense_date DATETIMEOFFSET(6) NOT NULL,
 amount DECIMAL(19,4) NOT NULL,
 currency_code VARCHAR(3) NOT NULL,
 notes NVARCHAR(2000) NULL,
 created_at DATETIMEOFFSET(6) NOT NULL,
 updated_at DATETIMEOFFSET(6) NOT NULL,
 deleted_at DATETIMEOFFSET(6) NULL,
 CONSTRAINT fk_expenses_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id),
 CONSTRAINT fk_expenses_currency FOREIGN KEY (currency_code) REFERENCES currencies(code),
 CONSTRAINT ck_expenses_category CHECK (category IN
   ('FUEL','MAINTENANCE','INSURANCE','REGISTRATION','PARKING','TOLL','WASH','REPAIR',
    'ACCESSORY','OTHER')),
 CONSTRAINT ck_expenses_amount CHECK (amount > 0),
 CONSTRAINT ck_expenses_title CHECK (LEN(LTRIM(RTRIM(title))) > 0)
);
CREATE INDEX ix_expenses_vehicle_id ON expenses(vehicle_id);
CREATE INDEX ix_expenses_expense_date ON expenses(expense_date DESC, created_at DESC, id DESC);
CREATE INDEX ix_expenses_category ON expenses(category);
CREATE INDEX ix_expenses_deleted_at ON expenses(deleted_at);
CREATE INDEX ix_expenses_vehicle_history
 ON expenses(vehicle_id, deleted_at, expense_date DESC, created_at DESC, id DESC);
