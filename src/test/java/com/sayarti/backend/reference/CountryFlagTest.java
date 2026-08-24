package com.sayarti.backend.reference;

import static org.assertj.core.api.Assertions.assertThat;
import com.sayarti.backend.reference.service.CountryFlag;
import org.junit.jupiter.api.Test;

class CountryFlagTest {
    @Test void convertsIsoCodes() {
        assertThat(CountryFlag.fromIsoCode("JO")).isEqualTo("🇯🇴");
        assertThat(CountryFlag.fromIsoCode("sa")).isEqualTo("🇸🇦");
        assertThat(CountryFlag.fromIsoCode("AE")).isEqualTo("🇦🇪");
        assertThat(CountryFlag.fromIsoCode("US")).isEqualTo("🇺🇸");
    }
    @Test void invalidCodesFailSafely() {
        assertThat(CountryFlag.fromIsoCode(null)).isNull();
        assertThat(CountryFlag.fromIsoCode("J")).isNull();
        assertThat(CountryFlag.fromIsoCode("123")).isNull();
    }
}
