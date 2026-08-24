package com.sayarti.backend.reference.dto;
import com.sayarti.backend.reference.entity.Country;
public record CountryResponse(String code, String name, String nameEn, String nameAr,
        String defaultCurrencyCode, String currencyCode, String flag) {
    public static CountryResponse from(Country value, boolean arabic) {
        return new CountryResponse(value.getCode(), arabic ? value.getNameAr() : value.getNameEn(),
                value.getNameEn(), value.getNameAr(), value.getDefaultCurrencyCode(),
                value.getDefaultCurrencyCode(), com.sayarti.backend.reference.service.CountryFlag.fromIsoCode(value.getCode()));
    }
}
