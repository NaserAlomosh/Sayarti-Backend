package com.sayarti.backend.reference.controller;
import com.sayarti.backend.common.response.ApiResponse;
import com.sayarti.backend.reference.dto.CountryResponse;
import com.sayarti.backend.reference.dto.CurrencyResponse;
import com.sayarti.backend.reference.service.ReferenceDataService;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController @RequestMapping("/api/v1/reference")
@Tag(name = "Reference data", description = "Public localized country and currency reference data. ISO codes are never translated.")
public class ReferenceController {
    private final ReferenceDataService service;
    public ReferenceController(ReferenceDataService service) { this.service = service; }
    @GetMapping("/countries")
    @Operation(summary = "List countries", description = "Returns code-sorted countries. name follows the resolved language; code and currencyCode remain stable ISO identifiers.")
    public ApiResponse<List<CountryResponse>> countries() { return ApiResponse.success(service.countries()); }
    @GetMapping("/currencies")
    @Operation(summary = "List currencies", description = "Returns code-sorted currencies. name and display symbol are localized; code remains the ISO 4217 identifier.")
    public ApiResponse<List<CurrencyResponse>> currencies() { return ApiResponse.success(service.currencies()); }
}
