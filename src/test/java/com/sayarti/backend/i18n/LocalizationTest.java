package com.sayarti.backend.i18n;

import static org.assertj.core.api.Assertions.assertThat;
import com.sayarti.backend.common.exception.ErrorCode;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;

class LocalizationTest {
    private final MessageLocalizer localizer = localizer();

    @AfterEach void reset() { LocaleContextHolder.resetLocaleContext(); }

    @Test void resolvesEnglishArabicRegionalAndUnsupportedLocales() {
        assertMessage(null, "Vehicle not found");
        assertMessage(Locale.ENGLISH, "Vehicle not found");
        assertMessage(Locale.forLanguageTag("en-US"), "Vehicle not found");
        assertMessage(Locale.forLanguageTag("ar"), "المركبة غير موجودة");
        assertMessage(Locale.forLanguageTag("ar-JO"), "المركبة غير موجودة");
        assertMessage(Locale.forLanguageTag("ar-SA"), "المركبة غير موجودة");
        assertMessage(Locale.FRENCH, "Vehicle not found");
    }

    @Test void machineReadableErrorCodeIsNeverLocalized() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("ar"));
        assertThat(ErrorCode.VEHICLE_NOT_FOUND.name()).isEqualTo("VEHICLE_NOT_FOUND");
        assertThat(localizer.error(ErrorCode.VEHICLE_NOT_FOUND, "Vehicle not found"))
                .isEqualTo("المركبة غير موجودة");
    }

    private void assertMessage(Locale locale, String expected) {
        if (locale == null) LocaleContextHolder.resetLocaleContext(); else LocaleContextHolder.setLocale(locale);
        assertThat(localizer.error(ErrorCode.VEHICLE_NOT_FOUND, "Vehicle not found")).isEqualTo(expected);
    }
    private static MessageLocalizer localizer() {
        var source = new ResourceBundleMessageSource(); source.setBasename("messages");
        source.setDefaultEncoding("UTF-8"); source.setDefaultLocale(Locale.ENGLISH);
        source.setFallbackToSystemLocale(false); return new MessageLocalizer(source);
    }
}
