package com.sayarti.backend.reference.service;
import com.sayarti.backend.common.exception.BusinessValidationException;
import com.sayarti.backend.common.exception.ErrorCode;
import com.sayarti.backend.reference.dto.CountryResponse;
import com.sayarti.backend.reference.dto.CurrencyResponse;
import com.sayarti.backend.reference.entity.Country;
import com.sayarti.backend.reference.entity.Currency;
import com.sayarti.backend.reference.repository.CountryRepository;
import com.sayarti.backend.reference.repository.CurrencyRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.i18n.LocaleContextHolder;
@Service
public class ReferenceDataService {
    private final CountryRepository countries; private final CurrencyRepository currencies;
    public ReferenceDataService(CountryRepository countries, CurrencyRepository currencies) { this.countries = countries; this.currencies = currencies; }
    @Transactional(readOnly = true) public Country requireCountry(String code) { return countries.findByCodeAndActiveTrue(normalize(code)).orElseThrow(() -> new BusinessValidationException(ErrorCode.COUNTRY_NOT_SUPPORTED, "Country is not supported")); }
    @Transactional(readOnly = true) public Currency requireCurrency(String code) { return currencies.findByCodeAndActiveTrue(normalize(code)).orElseThrow(() -> new BusinessValidationException(ErrorCode.CURRENCY_NOT_SUPPORTED, "Currency is not supported")); }
    @Transactional(readOnly = true) public List<CountryResponse> countries() { boolean ar = arabic(); return countries.findAllByActiveTrueOrderByCodeAsc().stream().map(v -> CountryResponse.from(v, ar)).toList(); }
    @Transactional(readOnly = true) public List<CurrencyResponse> currencies() { boolean ar = arabic(); return currencies.findAllByActiveTrueOrderByCodeAsc().stream().map(v -> CurrencyResponse.from(v, ar)).toList(); }
    private boolean arabic() { return "ar".equals(LocaleContextHolder.getLocale().getLanguage()); }
    private String normalize(String code) { return code == null ? "" : code.trim().toUpperCase(Locale.ROOT); }
}
