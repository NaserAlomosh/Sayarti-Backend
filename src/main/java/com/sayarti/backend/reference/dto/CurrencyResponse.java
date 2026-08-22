package com.sayarti.backend.reference.dto;
import com.sayarti.backend.reference.entity.Currency;
public record CurrencyResponse(String code, String nameEn, String nameAr, String symbol,
        int decimalDigits) {
    public static CurrencyResponse from(Currency value) { return new CurrencyResponse(value.getCode(), value.getNameEn(), value.getNameAr(), value.getSymbol(), value.getDecimalDigits()); }
}
