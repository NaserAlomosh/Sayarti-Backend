package com.sayarti.backend.fuel;

import static org.assertj.core.api.Assertions.assertThat;

import com.sayarti.backend.fuel.entity.FuelRecord;
import com.sayarti.backend.fuel.service.FuelCalculator;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FuelCalculatorTest {
    @Test
    void calculatesDistanceEfficiencyLitersPer100AndCostPerKm() {
        assertThat(FuelCalculator.distance(decimal("10500"), decimal("10000")))
                .isEqualByComparingTo("500.00");
        assertThat(FuelCalculator.kmPerLiter(decimal("500"), decimal("40")))
                .isEqualByComparingTo("12.5000");
        assertThat(FuelCalculator.litersPer100Km(decimal("500"), decimal("40")))
                .isEqualByComparingTo("8.0000");
        assertThat(FuelCalculator.costPerKm(decimal("60"), decimal("500")))
                .isEqualByComparingTo("0.1200");
    }

    @Test
    void unavailableCalculationsReturnNullAndNeverReturnNegativeDistance() {
        assertThat(FuelCalculator.distance(decimal("999"), decimal("1000"))).isNull();
        assertThat(FuelCalculator.distance(decimal("1000"), decimal("1000"))).isNull();
        assertThat(FuelCalculator.kmPerLiter(decimal("100"), BigDecimal.ZERO)).isNull();
        assertThat(FuelCalculator.kmPerLiter(BigDecimal.ZERO, decimal("10"))).isNull();
        assertThat(FuelCalculator.litersPer100Km(decimal("100"), BigDecimal.ZERO)).isNull();
        assertThat(FuelCalculator.costPerKm(decimal("10"), BigDecimal.ZERO)).isNull();
    }

    @Test
    void firstAndSingleRecordHaveNoEligibleDistanceOrEfficiency() {
        var result = FuelCalculator.calculate(List.of(record(1000, "40", "2", "2026-08-01")),
                YearMonth.of(2026, 8));
        assertThat(result.totalDistance()).isEqualByComparingTo("0.00");
        assertThat(result.averageKmPerLiter()).isNull();
        assertThat(result.averageLitersPer100Km()).isNull();
    }

    @Test
    void aggregateEfficiencyUsesTotalDistanceOverLaterRefillQuantities() {
        var result = FuelCalculator.calculate(List.of(
                record(1000, "30", "2", "2026-08-01"),
                record(1300, "20", "2", "2026-08-05"),
                record(1800, "50", "2", "2026-08-10")), YearMonth.of(2026, 8));
        assertThat(result.totalDistance()).isEqualByComparingTo("800.00");
        assertThat(result.averageKmPerLiter()).isEqualByComparingTo("11.4286");
        assertThat(result.averageLitersPer100Km()).isEqualByComparingTo("8.7500");
    }

    @Test
    void orderingUsesHistoricalDateRatherThanInsertionOrder() {
        var result = FuelCalculator.calculate(List.of(
                record(1800, "50", "2", "2026-08-10"),
                record(1000, "30", "2", "2026-08-01"),
                record(1300, "20", "2", "2026-08-05")), YearMonth.of(2026, 8));
        assertThat(result.totalDistance()).isEqualByComparingTo("800.00");
    }

    private FuelRecord record(long odometer, String quantity, String price, String date) {
        BigDecimal quantityValue = decimal(quantity);
        BigDecimal priceValue = decimal(price);
        return new FuelRecord(UUID.randomUUID(), decimal(Long.toString(odometer)), quantityValue,
                priceValue, quantityValue.multiply(priceValue), "USD",
                Instant.parse(date + "T10:00:00Z"), true, null, null);
    }

    private BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }
}
