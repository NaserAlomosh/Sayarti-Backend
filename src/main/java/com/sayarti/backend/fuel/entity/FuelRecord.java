package com.sayarti.backend.fuel.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Nationalized;

@Entity
@Table(name = "fuel_records")
public class FuelRecord {
    @Id private UUID id;
    @Column(name = "vehicle_id", nullable = false) private UUID vehicleId;
    @Column(name = "odometer_km", nullable = false, precision = 19, scale = 2)
    private BigDecimal odometerKm;
    @Column(name = "quantity_liters", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantityLiters;
    @Column(name = "price_per_liter", nullable = false, precision = 19, scale = 4)
    private BigDecimal pricePerLiter;
    @Column(name = "total_cost", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalCost;
    @Column(name = "currency_code", nullable = false, length = 3) private String currencyCode;
    @Column(name = "filled_at", nullable = false) private Instant filledAt;
    @Column(name = "full_tank", nullable = false) private boolean fullTank;
    @Nationalized @Column(name = "station_name", length = 200) private String stationName;
    @Nationalized @Column(length = 2000) private String notes;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(name = "deleted_at") private Instant deletedAt;

    protected FuelRecord() { }

    public FuelRecord(UUID vehicleId, BigDecimal odometerKm, BigDecimal quantityLiters,
            BigDecimal pricePerLiter, BigDecimal totalCost, String currencyCode, Instant filledAt,
            boolean fullTank, String stationName, String notes) {
        this.id = UUID.randomUUID();
        this.vehicleId = vehicleId;
        this.odometerKm = odometerKm;
        this.quantityLiters = quantityLiters;
        this.pricePerLiter = pricePerLiter;
        this.totalCost = totalCost;
        this.currencyCode = currencyCode;
        this.filledAt = filledAt;
        this.fullTank = fullTank;
        this.stationName = trim(stationName);
        this.notes = trim(notes);
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }

    public void update(BigDecimal quantity, BigDecimal price, BigDecimal total, String currency,
            Instant filledAt, boolean fullTank, String stationName, String notes) {
        this.quantityLiters = quantity;
        this.pricePerLiter = price;
        this.totalCost = total;
        this.currencyCode = currency;
        this.filledAt = filledAt;
        this.fullTank = fullTank;
        this.stationName = trim(stationName);
        this.notes = trim(notes);
        this.updatedAt = Instant.now();
    }
    public void delete() { deletedAt = Instant.now(); updatedAt = deletedAt; }
    private String trim(String value) { return value == null ? null : value.trim(); }
    public UUID getId() { return id; }
    public UUID getVehicleId() { return vehicleId; }
    public BigDecimal getOdometerKm() { return odometerKm; }
    public BigDecimal getQuantityLiters() { return quantityLiters; }
    public BigDecimal getPricePerLiter() { return pricePerLiter; }
    public BigDecimal getTotalCost() { return totalCost; }
    public String getCurrencyCode() { return currencyCode; }
    public Instant getFilledAt() { return filledAt; }
    public boolean isFullTank() { return fullTank; }
    public String getStationName() { return stationName; }
    public String getNotes() { return notes; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }
}
