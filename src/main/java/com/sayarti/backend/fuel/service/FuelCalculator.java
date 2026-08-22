package com.sayarti.backend.fuel.service;

import com.sayarti.backend.fuel.entity.FuelRecord;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class FuelCalculator {
    public static final int CALCULATION_SCALE = 4;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final Comparator<FuelRecord> HISTORICAL_ORDER = Comparator
            .comparing(FuelRecord::getFilledAt)
            .thenComparing(FuelRecord::getOdometerKm)
            .thenComparing(FuelRecord::getCreatedAt)
            .thenComparing(FuelRecord::getId);

    private FuelCalculator() {
    }

    public static Result calculate(List<FuelRecord> source, YearMonth month) {
        List<FuelRecord> records = source.stream()
                .filter(record -> record.getDeletedAt() == null)
                .sorted(HISTORICAL_ORDER)
                .toList();

        BigDecimal totalQuantity = records.stream()
                .map(FuelRecord::getQuantityLiters)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal eligibleDistance = BigDecimal.ZERO;
        BigDecimal eligibleQuantity = BigDecimal.ZERO;

        for (int index = 1; index < records.size(); index++) {
            FuelRecord previous = records.get(index - 1);
            FuelRecord current = records.get(index);

            BigDecimal distance = distance(
                    current.getOdometerKm(),
                    previous.getOdometerKm());

            if (distance != null && current.getQuantityLiters().signum() > 0) {
                eligibleDistance = eligibleDistance.add(distance);
                eligibleQuantity = eligibleQuantity.add(
                        current.getQuantityLiters());
            }
        }

        BigDecimal kmPerLiter = divide(
                eligibleDistance,
                eligibleQuantity);

        BigDecimal litersPer100 =
                eligibleDistance.signum() > 0
                        && eligibleQuantity.signum() > 0
                        ? eligibleQuantity
                        .multiply(BigDecimal.valueOf(100))
                        .divide(
                                eligibleDistance,
                                CALCULATION_SCALE,
                                ROUNDING)
                        : null;

        Map<String, BigDecimal> totals = new TreeMap<>();
        Map<String, BigDecimal> monthly = new TreeMap<>();

        Instant from = month
                .atDay(1)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();

        Instant until = month
                .plusMonths(1)
                .atDay(1)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();

        for (FuelRecord record : records) {
            totals.merge(
                    record.getCurrencyCode(),
                    record.getTotalCost(),
                    BigDecimal::add);

            if (!record.getFilledAt().isBefore(from)
                    && record.getFilledAt().isBefore(until)) {
                monthly.merge(
                        record.getCurrencyCode(),
                        record.getTotalCost(),
                        BigDecimal::add);
            }
        }

        List<CostResult> costs = new ArrayList<>();

        for (Map.Entry<String, BigDecimal> entry : totals.entrySet()) {
            String currency = entry.getKey();
            BigDecimal totalCost = entry.getValue();

            BigDecimal monthlyCost = monthly.getOrDefault(
                    currency,
                    BigDecimal.ZERO);

            BigDecimal costPerKilometer =
                    eligibleDistance.signum() > 0
                            ? totalCost.divide(
                            eligibleDistance,
                            CALCULATION_SCALE,
                            ROUNDING)
                            : null;

            costs.add(
                    new CostResult(
                            currency,
                            money(totalCost),
                            money(monthlyCost),
                            costPerKilometer));
        }

        return new Result(
                quantity(totalQuantity),
                distanceScale(eligibleDistance),
                kmPerLiter,
                litersPer100,
                List.copyOf(costs));
    }

    public static BigDecimal distance(BigDecimal currentMileage, BigDecimal previousMileage) {
        if (currentMileage == null || previousMileage == null) {
            return null;
        }
        BigDecimal distance = currentMileage.subtract(previousMileage);
        return distance.signum() > 0 ? distanceScale(distance) : null;
    }

    public static BigDecimal kmPerLiter(BigDecimal distance, BigDecimal quantity) {
        return divide(distance, quantity);
    }

    public static BigDecimal litersPer100Km(BigDecimal distance, BigDecimal quantity) {
        if (!positive(distance) || !positive(quantity)) {
            return null;
        }
        return quantity.multiply(BigDecimal.valueOf(100))
                .divide(distance, CALCULATION_SCALE, ROUNDING);
    }

    public static BigDecimal costPerKm(BigDecimal cost, BigDecimal distance) {
        return !positive(cost) || !positive(distance) ? null
                : cost.divide(distance, CALCULATION_SCALE, ROUNDING);
    }

    private static BigDecimal divide(BigDecimal numerator, BigDecimal denominator) {
        return !positive(numerator) || !positive(denominator) ? null
                : numerator.divide(denominator, CALCULATION_SCALE, ROUNDING);
    }

    private static boolean positive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(4, ROUNDING);
    }

    private static BigDecimal quantity(BigDecimal value) {
        return value.setScale(3, ROUNDING);
    }

    private static BigDecimal distanceScale(BigDecimal value) {
        return value.setScale(2, ROUNDING);
    }

    public record Result(BigDecimal totalQuantity, BigDecimal totalDistance,
                         BigDecimal averageKmPerLiter, BigDecimal averageLitersPer100Km,
                         List<CostResult> costs) {
    }

    public record CostResult(String currencyCode, BigDecimal totalCost, BigDecimal monthlyCost,
                             BigDecimal costPerKm) {
    }
}
