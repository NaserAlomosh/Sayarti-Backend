package com.sayarti.backend.i18n;

import com.sayarti.backend.security.jwt.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Locale;
import org.springframework.lang.Nullable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.LocaleResolver;

/** Central language policy: an explicit supported header, then the user preference, then English. */
public final class SayartiLocaleResolver implements LocaleResolver {
    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        Locale header = supportedHeader(request);
        if (header != null) {
            return header;
        }
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return fromLanguage(user.preferredLanguage());
        }
        return LocalizationConfig.ENGLISH;
    }

    public static Locale resolveHeader(HttpServletRequest request) {
        Locale locale = supportedHeader(request);
        return locale == null ? LocalizationConfig.ENGLISH : locale;
    }

    @Override
    public void setLocale(HttpServletRequest request, @Nullable HttpServletResponse response,
            @Nullable Locale locale) {
        throw new UnsupportedOperationException("Sayarti locale is request-resolved");
    }

    public static Locale fromLanguage(String language) {
        return "ar".equalsIgnoreCase(language) ? LocalizationConfig.ARABIC : LocalizationConfig.ENGLISH;
    }

    private static Locale supportedHeader(HttpServletRequest request) {
        String value = request.getHeader("Accept-Language");
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            for (Locale.LanguageRange range : Locale.LanguageRange.parse(value)) {
                String language = Locale.forLanguageTag(range.getRange()).getLanguage();
                if ("ar".equalsIgnoreCase(language)) return LocalizationConfig.ARABIC;
                if ("en".equalsIgnoreCase(language)) return LocalizationConfig.ENGLISH;
            }
        } catch (IllegalArgumentException ignored) {
            // A malformed header follows the same safe English fallback as an unsupported one.
        }
        return LocalizationConfig.ENGLISH;
    }
}
