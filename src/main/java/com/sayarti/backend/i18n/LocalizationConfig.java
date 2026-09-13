package com.sayarti.backend.i18n;

import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;

@Configuration
public class LocalizationConfig {
    public static final Locale ENGLISH = Locale.ENGLISH;
    public static final Locale ARABIC = Locale.forLanguageTag("ar");

    @Bean
    MessageSource messageSource() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding("UTF-8");
        source.setDefaultLocale(ENGLISH);
        source.setFallbackToSystemLocale(false);
        return source;
    }

    @Bean
    LocaleResolver localeResolver() {
        return new SayartiLocaleResolver();
    }
}
