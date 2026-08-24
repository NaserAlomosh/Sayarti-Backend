package com.sayarti.backend.reference.dto;
import com.sayarti.backend.reference.entity.Currency;
public record CurrencyResponse(String code, String name, String nameEn, String nameAr, String symbol,
        int decimalDigits) {
    public static CurrencyResponse from(Currency value, boolean arabic) { return new CurrencyResponse(value.getCode(),
            arabic ? value.getNameAr() : value.getNameEn(), value.getNameEn(), value.getNameAr(),
            arabic ? value.getSymbolAr() : value.getSymbolEn(), value.getDecimalDigits()); }
}
