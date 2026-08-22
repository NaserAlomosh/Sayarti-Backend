package com.sayarti.backend.reminder.repository;

import com.sayarti.backend.reminder.entity.Reminder;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReminderRepository extends JpaRepository<Reminder, UUID> {
    List<Reminder> findAllByVehicleIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(UUID vehicleId);
    Optional<Reminder> findByIdAndVehicleIdAndDeletedAtIsNull(UUID id, UUID vehicleId);
    List<Reminder> findAllByVehicleIdAndCompletedFalseAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(
            UUID vehicleId);
}
