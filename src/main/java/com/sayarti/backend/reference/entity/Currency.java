package com.sayarti.backend.reference.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Nationalized;

@Entity
@Table(name = "currencies")
public class Currency {
    @Id @Column(length = 3) private String code;
    @Nationalized @Column(name = "name_en", nullable = false, length = 100) private String nameEn;
    @Nationalized @Column(name = "name_ar", nullable = false, length = 100) private String nameAr;
    @Nationalized @Column(name = "symbol_en", nullable = false, length = 10) private String symbolEn;
    @Nationalized @Column(name = "symbol_ar", nullable = false, length = 10) private String symbolAr;
    @Column(name = "decimal_digits", nullable = false) private int decimalDigits;
    @Column(nullable = false) private boolean active;
    protected Currency() { }
    public String getCode() { return code; }
    public String getNameEn() { return nameEn; }
    public String getNameAr() { return nameAr; }
    public String getSymbolEn() { return symbolEn; }
    public String getSymbolAr() { return symbolAr; }
    public int getDecimalDigits() { return decimalDigits; }
    public boolean isActive() { return active; }
}
