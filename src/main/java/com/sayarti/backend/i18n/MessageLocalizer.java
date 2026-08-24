package com.sayarti.backend.i18n;

import com.sayarti.backend.common.exception.ErrorCode;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

@Component
public class MessageLocalizer {
    private final MessageSource messages;

    public MessageLocalizer(MessageSource messages) {
        this.messages = messages;
    }

    public String error(ErrorCode code, String englishFallback) {
        return get("error." + code.name().toLowerCase(Locale.ROOT).replace('_', '.'),
                englishFallback, LocaleContextHolder.getLocale());
    }

    public String text(String key, String englishFallback, Object... arguments) {
        return messages.getMessage(key, arguments, englishFallback, supported(LocaleContextHolder.getLocale()));
    }

    public String text(String key, String englishFallback, Locale locale, Object... arguments) {
        return messages.getMessage(key, arguments, englishFallback, supported(locale));
    }

    public Locale supported(Locale locale) {
        return locale != null && "ar".equalsIgnoreCase(locale.getLanguage())
                ? LocalizationConfig.ARABIC : LocalizationConfig.ENGLISH;
    }

    private String get(String key, String fallback, Locale locale) {
        return messages.getMessage(key, null, fallback, supported(locale));
    }
}
