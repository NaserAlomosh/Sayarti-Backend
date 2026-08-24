package com.sayarti.backend.vehicle.entity;

import com.sayarti.backend.common.persistence.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Nationalized;

@Entity
@Table(name = "vehicles")
public class Vehicle extends BaseAuditableEntity {
    @Id
    private UUID id;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Nationalized
    @Column(nullable = false, length = 100)
    private String brand;
    @Nationalized
    @Column(nullable = false, length = 100)
    private String model;
    @Column(name = "vehicle_year", nullable = false) private int year;
    @Enumerated(EnumType.STRING) @Column(name = "powertrain_type", nullable = false, length = 30)
    private PowertrainType powertrainType;
    @Column(name = "current_mileage", nullable = false) private long currentMileage;
    @Nationalized @Column(name = "license_plate", length = 30) private String licensePlate;
    @Nationalized @Column(length = 100) private String nickname;
    @Nationalized @Column(name = "image_url", length = 2048) private String imageUrl;
    @Enumerated(EnumType.STRING) @Column(name = "fuel_type", length = 20)
    private FuelType fuelType;
    @Column(name = "fuel_tank_capacity_liters", precision = 10, scale = 2)
    private BigDecimal fuelTankCapacityLiters;
    @Column(name = "battery_capacity_kwh", precision = 10, scale = 2)
    private BigDecimal batteryCapacityKwh;
    @Column(name = "estimated_range_km", precision = 10, scale = 2)
    private BigDecimal estimatedRangeKm;
    @Column(name = "deleted_at") private Instant deletedAt;

    protected Vehicle() { }

    public Vehicle(UUID userId, String brand, String model, int year,
            PowertrainType powertrainType, long currentMileage, String licensePlate,
            String nickname, String imageUrl, FuelType fuelType,
            BigDecimal fuelTankCapacityLiters, BigDecimal batteryCapacityKwh,
            BigDecimal estimatedRangeKm) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        updateDetails(brand, model, year, powertrainType, licensePlate, nickname, imageUrl,
                fuelType, fuelTankCapacityLiters, batteryCapacityKwh, estimatedRangeKm);
        this.currentMileage = currentMileage;
    }

    public void updateDetails(String brand, String model, int year,
            PowertrainType powertrainType, String licensePlate, String nickname, String imageUrl,
            FuelType fuelType, BigDecimal fuelTankCapacityLiters, BigDecimal batteryCapacityKwh,
            BigDecimal estimatedRangeKm) {
        this.brand = brand.trim();
        this.model = model.trim();
        this.year = year;
        this.powertrainType = powertrainType;
        this.licensePlate = trim(licensePlate);
        this.nickname = trim(nickname);
        this.imageUrl = trim(imageUrl);
        this.fuelType = fuelType;
        this.fuelTankCapacityLiters = fuelTankCapacityLiters;
        this.batteryCapacityKwh = batteryCapacityKwh;
        this.estimatedRangeKm = estimatedRangeKm;
    }
    public void updateMileage(long mileage) { this.currentMileage = mileage; }
    public void delete() { this.deletedAt = Instant.now(); }
    private String trim(String value) { return value == null ? null : value.trim(); }

    public UUID getId() { return id; } public UUID getUserId() { return userId; }
    public String getBrand() { return brand; } public String getModel() { return model; }
    public int getYear() { return year; } public PowertrainType getPowertrainType() { return powertrainType; }
    public long getCurrentMileage() { return currentMileage; } public String getLicensePlate() { return licensePlate; }
    public String getNickname() { return nickname; } public String getImageUrl() { return imageUrl; }
    public FuelType getFuelType() { return fuelType; }
    public BigDecimal getFuelTankCapacityLiters() { return fuelTankCapacityLiters; }
    public BigDecimal getBatteryCapacityKwh() { return batteryCapacityKwh; }
    public BigDecimal getEstimatedRangeKm() { return estimatedRangeKm; }
    public Instant getDeletedAt() { return deletedAt; }
}
