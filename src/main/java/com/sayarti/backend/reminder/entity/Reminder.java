package com.sayarti.backend.reminder.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Nationalized;

@Entity
@Table(name = "reminders")
public class Reminder {
    @Id private UUID id;
    @Column(name = "vehicle_id", nullable = false) private UUID vehicleId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private ReminderCategory category;
    @Nationalized @Column(nullable = false, length = 200) private String title;
    @Nationalized @Column(length = 2000) private String description;
    @Enumerated(EnumType.STRING) @Column(name = "trigger_type", nullable = false, length = 10)
    private ReminderTriggerType triggerType;
    @Column(name = "target_date") private Instant targetDate;
    @Column(name = "target_mileage") private Long targetMileage;
    @Column(nullable = false) private boolean completed;
    @Column(name = "completed_at") private Instant completedAt;
    @Column(name = "notification_delivered_at") private Instant notificationDeliveredAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(name = "deleted_at") private Instant deletedAt;

    protected Reminder() { }

    public Reminder(UUID vehicleId, ReminderCategory category, String title, String description,
            ReminderTriggerType triggerType, Instant targetDate, Long targetMileage) {
        this.id = UUID.randomUUID();
        this.vehicleId = vehicleId;
        this.completed = false;
        this.createdAt = Instant.now();
        update(category, title, description, triggerType, targetDate, targetMileage);
        this.createdAt = this.updatedAt;
    }

    public void update(ReminderCategory category, String title, String description,
            ReminderTriggerType triggerType, Instant targetDate, Long targetMileage) {
        this.category = category;
        this.title = title.trim();
        this.description = description == null ? null : description.trim();
        this.triggerType = triggerType;
        this.targetDate = targetDate;
        this.targetMileage = targetMileage;
        this.updatedAt = Instant.now();
    }

    public void complete() {
        if (!completed) {
            completed = true;
            completedAt = Instant.now();
            updatedAt = completedAt;
        }
    }

    public void delete() { deletedAt = Instant.now(); updatedAt = deletedAt; }
    public void markNotificationDelivered(Instant deliveredAt) {
        notificationDeliveredAt = deliveredAt;
    }
    public UUID getId() { return id; }
    public UUID getVehicleId() { return vehicleId; }
    public ReminderCategory getCategory() { return category; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public ReminderTriggerType getTriggerType() { return triggerType; }
    public Instant getTargetDate() { return targetDate; }
    public Long getTargetMileage() { return targetMileage; }
    public boolean isCompleted() { return completed; }
    public Instant getCompletedAt() { return completedAt; }
    public Instant getNotificationDeliveredAt() { return notificationDeliveredAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }
}
