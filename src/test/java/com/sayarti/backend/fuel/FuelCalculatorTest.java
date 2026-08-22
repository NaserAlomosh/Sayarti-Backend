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

    @Test
    void odometerBreaksTiesWhenRecordsHaveTheSameHistoricalTimestamp() {
        var result = FuelCalculator.calculate(List.of(
                record(1500, "40", "2", "USD", "2026-08-01"),
                record(1000, "30", "2", "USD", "2026-08-01"),
                record(1800, "20", "2", "USD", "2026-08-02")),
                YearMonth.of(2026, 8));

        assertThat(result.totalDistance()).isEqualByComparingTo("800.00");
        assertThat(result.averageKmPerLiter()).isEqualByComparingTo("13.3333");
    }

    @Test
    void emptyInputHasZeroTotalsAndNoDerivedValues() {
        var result = FuelCalculator.calculate(List.of(), YearMonth.of(2026, 8));

        assertThat(result.totalQuantity()).isEqualByComparingTo("0.000");
        assertThat(result.totalDistance()).isEqualByComparingTo("0.00");
        assertThat(result.averageKmPerLiter()).isNull();
        assertThat(result.averageLitersPer100Km()).isNull();
        assertThat(result.costs()).isEmpty();
    }

    @Test
    void decreasingAndDuplicateOdometersDoNotCreateEligibleDistance() {
        var result = FuelCalculator.calculate(List.of(
                record(1000, "20", "2", "2026-08-01"),
                record(900, "30", "2", "2026-08-02"),
                record(900, "40", "2", "2026-08-03")), YearMonth.of(2026, 8));

        assertThat(result.totalDistance()).isEqualByComparingTo("0.00");
        assertThat(result.averageKmPerLiter()).isNull();
        assertThat(result.averageLitersPer100Km()).isNull();
        assertThat(result.costs().get(0).costPerKm()).isNull();
    }

    @Test
    void deletedRecordsAreExcludedFromEveryAggregate() {
        FuelRecord deleted = record(1300, "100", "5", "2026-08-05");
        deleted.delete();

        var result = FuelCalculator.calculate(List.of(
                record(1000, "30", "2", "2026-08-01"),
                deleted,
                record(1500, "50", "2", "2026-08-10")), YearMonth.of(2026, 8));

        assertThat(result.totalQuantity()).isEqualByComparingTo("80.000");
        assertThat(result.totalDistance()).isEqualByComparingTo("500.00");
        assertThat(result.averageKmPerLiter()).isEqualByComparingTo("10.0000");
        assertThat(result.costs().get(0).totalCost()).isEqualByComparingTo("160.0000");
    }

    @Test
    void costsRemainSeparatedByCurrencyAndMonthUsesUtcBoundaries() {
        var result = FuelCalculator.calculate(List.of(
                record(1000, "10", "2", "USD", "2026-07-31"),
                record(1100, "10", "3", "EUR", "2026-08-01"),
                record(1200, "10", "4", "USD", "2026-09-01")),
                YearMonth.of(2026, 8));

        assertThat(result.costs()).extracting(FuelCalculator.CostResult::currencyCode)
                .containsExactly("EUR", "USD");
        assertThat(result.costs().get(0).totalCost()).isEqualByComparingTo("30.0000");
        assertThat(result.costs().get(0).monthlyCost()).isEqualByComparingTo("30.0000");
        assertThat(result.costs().get(1).totalCost()).isEqualByComparingTo("60.0000");
        assertThat(result.costs().get(1).monthlyCost()).isEqualByComparingTo("0.0000");
        assertThat(result.costs().get(0).costPerKm()).isEqualByComparingTo("0.1500");
        assertThat(result.costs().get(1).costPerKm()).isEqualByComparingTo("0.3000");
    }

    private FuelRecord record(long odometer, String quantity, String price, String date) {
        return record(odometer, quantity, price, "USD", date);
    }

    private FuelRecord record(long odometer, String quantity, String price, String currency,
            String date) {
        BigDecimal quantityValue = decimal(quantity);
        BigDecimal priceValue = decimal(price);
        return new FuelRecord(UUID.randomUUID(), decimal(Long.toString(odometer)), quantityValue,
                priceValue, quantityValue.multiply(priceValue), currency,
                Instant.parse(date + "T10:00:00Z"), true, null, null);
    }

    private BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }
}
