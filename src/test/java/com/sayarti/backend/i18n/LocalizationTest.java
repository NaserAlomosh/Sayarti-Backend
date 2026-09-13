package com.sayarti.backend.i18n;

import static org.assertj.core.api.Assertions.assertThat;
import com.sayarti.backend.common.exception.ErrorCode;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class LocalizationTest {
    private final MessageLocalizer localizer = localizer();

    @AfterEach void reset() {
        LocaleContextHolder.resetLocaleContext();
        SecurityContextHolder.clearContext();
    }

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

    @Test void requestLanguageUsesHeaderThenUserPreferenceThenEnglish() {
        SayartiLocaleResolver resolver = new SayartiLocaleResolver();
        MockHttpServletRequest request = new MockHttpServletRequest();
        assertThat(resolver.resolveLocale(request)).isEqualTo(Locale.ENGLISH);

        var user = new com.sayarti.backend.security.jwt.AuthenticatedUser(
                UUID.randomUUID(), "user@example.com", "ar");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null));
        assertThat(resolver.resolveLocale(request)).isEqualTo(LocalizationConfig.ARABIC);

        request.addHeader("Accept-Language", "en-US");
        assertThat(resolver.resolveLocale(request)).isEqualTo(Locale.ENGLISH);
    }

    @Test void unsupportedAndMalformedHeadersFallBackToEnglish() {
        SayartiLocaleResolver resolver = new SayartiLocaleResolver();
        MockHttpServletRequest unsupported = new MockHttpServletRequest();
        unsupported.addHeader("Accept-Language", "fr-FR");
        assertThat(resolver.resolveLocale(unsupported)).isEqualTo(Locale.ENGLISH);
        MockHttpServletRequest malformed = new MockHttpServletRequest();
        malformed.addHeader("Accept-Language", "not a valid language header (");
        assertThat(resolver.resolveLocale(malformed)).isEqualTo(Locale.ENGLISH);
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
