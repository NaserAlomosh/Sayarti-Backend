package com.sayarti.backend.maintenance.entity;

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
@Table(name = "maintenance_records")
public class MaintenanceRecord {
    @Id private UUID id;
    @Column(name = "vehicle_id", nullable = false) private UUID vehicleId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private MaintenanceCategory category;
    @Nationalized @Column(nullable = false, length = 200) private String title;
    @Column(name = "service_date", nullable = false) private Instant serviceDate;
    @Column(name = "mileage_km", nullable = false, precision = 19, scale = 2)
    private BigDecimal mileageKm;
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal cost;
    @Column(name = "currency_code", nullable = false, length = 3) private String currencyCode;
    @Nationalized @Column(name = "service_provider", length = 200) private String serviceProvider;
    @Nationalized @Column(length = 2000) private String notes;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(name = "deleted_at") private Instant deletedAt;

    protected MaintenanceRecord() { }

    public MaintenanceRecord(UUID vehicleId, MaintenanceCategory category, String title,
            Instant serviceDate, BigDecimal mileageKm, BigDecimal cost, String currencyCode,
            String serviceProvider, String notes) {
        this.id = UUID.randomUUID();
        this.vehicleId = vehicleId;
        this.createdAt = Instant.now();
        update(category, title, serviceDate, mileageKm, cost, currencyCode, serviceProvider, notes);
        this.createdAt = this.updatedAt;
    }

    public void update(MaintenanceCategory category, String title, Instant serviceDate,
            BigDecimal mileageKm, BigDecimal cost, String currencyCode, String serviceProvider,
            String notes) {
        this.category = category;
        this.title = title.trim();
        this.serviceDate = serviceDate;
        this.mileageKm = mileageKm;
        this.cost = cost;
        this.currencyCode = currencyCode;
        this.serviceProvider = trim(serviceProvider);
        this.notes = trim(notes);
        this.updatedAt = Instant.now();
    }

    public void delete() { deletedAt = Instant.now(); updatedAt = deletedAt; }
    private String trim(String value) { return value == null ? null : value.trim(); }
    public UUID getId() { return id; }
    public UUID getVehicleId() { return vehicleId; }
    public MaintenanceCategory getCategory() { return category; }
    public String getTitle() { return title; }
    public Instant getServiceDate() { return serviceDate; }
    public BigDecimal getMileageKm() { return mileageKm; }
    public BigDecimal getCost() { return cost; }
    public String getCurrencyCode() { return currencyCode; }
    public String getServiceProvider() { return serviceProvider; }
    public String getNotes() { return notes; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }
}
