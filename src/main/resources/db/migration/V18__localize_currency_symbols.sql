ALTER TABLE currencies ADD symbol_en NVARCHAR(10) NULL, symbol_ar NVARCHAR(10) NULL;

EXEC sp_executesql N'
UPDATE currencies SET symbol_en = symbol, symbol_ar = symbol;

UPDATE currencies SET symbol_en = N''AED'', symbol_ar = N''د.إ'' WHERE code = ''AED'';
UPDATE currencies SET symbol_en = N''BD'',  symbol_ar = N''د.ب'' WHERE code = ''BHD'';
UPDATE currencies SET symbol_en = N''EGP'', symbol_ar = N''ج.م'' WHERE code = ''EGP'';
UPDATE currencies SET symbol_en = N''JD'',  symbol_ar = N''د.أ'' WHERE code = ''JOD'';
UPDATE currencies SET name_ar = N''الدينار الأردني'' WHERE code = ''JOD'';
UPDATE currencies SET symbol_en = N''KD'',  symbol_ar = N''د.ك'' WHERE code = ''KWD'';
UPDATE currencies SET symbol_en = N''MAD'', symbol_ar = N''د.م.'' WHERE code = ''MAD'';
UPDATE currencies SET symbol_en = N''OMR'', symbol_ar = N''ر.ع.'' WHERE code = ''OMR'';
UPDATE currencies SET symbol_en = N''QAR'', symbol_ar = N''ر.ق'' WHERE code = ''QAR'';
UPDATE currencies SET symbol_en = N''SAR'', symbol_ar = N''ر.س'' WHERE code = ''SAR'';

ALTER TABLE currencies ALTER COLUMN symbol_en NVARCHAR(10) NOT NULL;
ALTER TABLE currencies ALTER COLUMN symbol_ar NVARCHAR(10) NOT NULL;
';

ALTER TABLE currencies DROP COLUMN symbol;
