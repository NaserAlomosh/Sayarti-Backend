package com.sayarti.backend.reference.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Nationalized;

@Entity
@Table(name = "countries")
public class Country {
    @Id @Column(length = 2) private String code;
    @Nationalized @Column(name = "name_en", nullable = false, length = 100) private String nameEn;
    @Nationalized @Column(name = "name_ar", nullable = false, length = 100) private String nameAr;
    @Column(name = "default_currency_code", nullable = false, length = 3) private String defaultCurrencyCode;
    @Column(nullable = false) private boolean active;
    protected Country() { }
    public String getCode() { return code; }
    public String getNameEn() { return nameEn; }
    public String getNameAr() { return nameAr; }
    public String getDefaultCurrencyCode() { return defaultCurrencyCode; }
    public boolean isActive() { return active; }
}
