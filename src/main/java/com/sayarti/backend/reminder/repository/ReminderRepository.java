package com.sayarti.backend.reminder.repository;

import com.sayarti.backend.reminder.entity.Reminder;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReminderRepository extends JpaRepository<Reminder, UUID> {
    List<Reminder> findAllByVehicleIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(UUID vehicleId);
    Optional<Reminder> findByIdAndVehicleIdAndDeletedAtIsNull(UUID id, UUID vehicleId);
    boolean existsByVehicleIdAndTitle(UUID vehicleId, String title);
    List<Reminder> findAllByVehicleIdAndDeletedAtIsNull(UUID vehicleId);
    List<Reminder> findAllByVehicleIdAndCompletedFalseAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(
            UUID vehicleId);
    List<Reminder> findByVehicleIdAndCompletedTrueAndCompletedAtIsNotNullAndDeletedAtIsNullOrderByCompletedAtDescIdDesc(
            UUID vehicleId, Pageable pageable);

    @Query("""
            select r.id from Reminder r, Vehicle v
            where r.vehicleId = v.id and r.completed = false and r.deletedAt is null
              and r.notificationDeliveredAt is null and v.deletedAt is null
              and ((r.triggerType = com.sayarti.backend.reminder.entity.ReminderTriggerType.DATE
                    and r.targetDate <= :now)
                or (r.triggerType = com.sayarti.backend.reminder.entity.ReminderTriggerType.MILEAGE
                    and v.currentMileage >= r.targetMileage))
            order by r.createdAt, r.id
            """)
    List<UUID> findDueReminderIds(@Param("now") java.time.Instant now, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reminder r where r.id = :id")
    Optional<Reminder> findByIdForNotification(@Param("id") UUID id);
}
