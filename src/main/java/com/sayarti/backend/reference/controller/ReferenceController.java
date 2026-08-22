package com.sayarti.backend.reference.controller;
import com.sayarti.backend.common.response.ApiResponse;
import com.sayarti.backend.reference.dto.CountryResponse;
import com.sayarti.backend.reference.dto.CurrencyResponse;
import com.sayarti.backend.reference.service.ReferenceDataService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController @RequestMapping("/api/v1/reference")
public class ReferenceController {
    private final ReferenceDataService service;
    public ReferenceController(ReferenceDataService service) { this.service = service; }
    @GetMapping("/countries") public ApiResponse<List<CountryResponse>> countries() { return ApiResponse.success(service.countries()); }
    @GetMapping("/currencies") public ApiResponse<List<CurrencyResponse>> currencies() { return ApiResponse.success(service.currencies()); }
}
