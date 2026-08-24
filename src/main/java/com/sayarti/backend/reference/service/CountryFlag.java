package com.sayarti.backend.reference.service;

import java.util.Locale;

public final class CountryFlag {
    private CountryFlag() { }

    public static String fromIsoCode(String code) {
        if (code == null || !code.matches("[A-Za-z]{2}")) return null;
        String upper = code.toUpperCase(Locale.ROOT);
        return new String(Character.toChars(0x1F1E6 + upper.charAt(0) - 'A'))
                + new String(Character.toChars(0x1F1E6 + upper.charAt(1) - 'A'));
    }
}
