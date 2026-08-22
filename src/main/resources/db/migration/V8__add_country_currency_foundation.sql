CREATE TABLE currencies (
 code VARCHAR(3) NOT NULL PRIMARY KEY,
 name_en NVARCHAR(100) NOT NULL,
 name_ar NVARCHAR(100) NOT NULL,
 symbol NVARCHAR(10) NOT NULL,
 decimal_digits INT NOT NULL,
 active BIT NOT NULL CONSTRAINT df_currencies_active DEFAULT 1,
 CONSTRAINT ck_currencies_decimal_digits CHECK (decimal_digits BETWEEN 0 AND 4)
);

INSERT INTO currencies (code,name_en,name_ar,symbol,decimal_digits,active) VALUES
('AED',N'UAE Dirham',N'درهم إماراتي',N'د.إ',2,1),
('BHD',N'Bahraini Dinar',N'دينار بحريني',N'د.ب',3,1),
('CAD',N'Canadian Dollar',N'دولار كندي',N'C$',2,1),
('CHF',N'Swiss Franc',N'فرنك سويسري',N'CHF',2,1),
('CNY',N'Chinese Yuan',N'يوان صيني',N'¥',2,1),
('EGP',N'Egyptian Pound',N'جنيه مصري',N'ج.م',2,1),
('EUR',N'Euro',N'يورو',N'€',2,1),
('GBP',N'Pound Sterling',N'جنيه إسترليني',N'£',2,1),
('INR',N'Indian Rupee',N'روبية هندية',N'₹',2,1),
('JOD',N'Jordanian Dinar',N'دينار أردني',N'د.أ',3,1),
('JPY',N'Japanese Yen',N'ين ياباني',N'¥',0,1),
('KWD',N'Kuwaiti Dinar',N'دينار كويتي',N'د.ك',3,1),
('MAD',N'Moroccan Dirham',N'درهم مغربي',N'د.م.',2,1),
('OMR',N'Omani Rial',N'ريال عماني',N'ر.ع.',3,1),
('QAR',N'Qatari Riyal',N'ريال قطري',N'ر.ق',2,1),
('SAR',N'Saudi Riyal',N'ريال سعودي',N'ر.س',2,1),
('TRY',N'Turkish Lira',N'ليرة تركية',N'₺',2,1),
('USD',N'US Dollar',N'دولار أمريكي',N'$',2,1);

CREATE TABLE countries (
 code VARCHAR(2) NOT NULL PRIMARY KEY,
 name_en NVARCHAR(100) NOT NULL,
 name_ar NVARCHAR(100) NOT NULL,
 default_currency_code VARCHAR(3) NOT NULL,
 active BIT NOT NULL CONSTRAINT df_countries_active DEFAULT 1,
 CONSTRAINT fk_countries_default_currency FOREIGN KEY (default_currency_code) REFERENCES currencies(code)
);

INSERT INTO countries (code,name_en,name_ar,default_currency_code,active) VALUES
('AE',N'United Arab Emirates',N'الإمارات العربية المتحدة','AED',1),('BH',N'Bahrain',N'البحرين','BHD',1),
('CA',N'Canada',N'كندا','CAD',1),('CH',N'Switzerland',N'سويسرا','CHF',1),
('CN',N'China',N'الصين','CNY',1),('DE',N'Germany',N'ألمانيا','EUR',1),
('EG',N'Egypt',N'مصر','EGP',1),('ES',N'Spain',N'إسبانيا','EUR',1),
('FR',N'France',N'فرنسا','EUR',1),('GB',N'United Kingdom',N'المملكة المتحدة','GBP',1),
('IN',N'India',N'الهند','INR',1),('IT',N'Italy',N'إيطاليا','EUR',1),
('JO',N'Jordan',N'الأردن','JOD',1),('JP',N'Japan',N'اليابان','JPY',1),
('KW',N'Kuwait',N'الكويت','KWD',1),('MA',N'Morocco',N'المغرب','MAD',1),
('OM',N'Oman',N'عُمان','OMR',1),('QA',N'Qatar',N'قطر','QAR',1),
('SA',N'Saudi Arabia',N'المملكة العربية السعودية','SAR',1),('TR',N'Türkiye',N'تركيا','TRY',1),
('US',N'United States',N'الولايات المتحدة','USD',1);

ALTER TABLE users ADD country_code VARCHAR(2) NULL, default_currency_code VARCHAR(3) NULL;
ALTER TABLE users ADD CONSTRAINT fk_users_country FOREIGN KEY (country_code) REFERENCES countries(code);
ALTER TABLE users ADD CONSTRAINT fk_users_default_currency FOREIGN KEY (default_currency_code) REFERENCES currencies(code);
CREATE INDEX ix_users_country_code ON users(country_code);
CREATE INDEX ix_users_default_currency_code ON users(default_currency_code);
CREATE INDEX ix_countries_default_currency_code ON countries(default_currency_code);
