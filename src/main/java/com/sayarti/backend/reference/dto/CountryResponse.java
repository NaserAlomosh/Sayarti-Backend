package com.sayarti.backend.reference.dto;
import com.sayarti.backend.reference.entity.Country;
public record CountryResponse(String code, String nameEn, String nameAr, String defaultCurrencyCode) {
    public static CountryResponse from(Country value) { return new CountryResponse(value.getCode(), value.getNameEn(), value.getNameAr(), value.getDefaultCurrencyCode()); }
}
