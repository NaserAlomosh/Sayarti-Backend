package com.sayarti.backend.reminder.service;

import com.sayarti.backend.common.exception.BusinessValidationException;
import com.sayarti.backend.common.exception.ErrorCode;
import com.sayarti.backend.common.exception.ResourceNotFoundException;
import com.sayarti.backend.reminder.dto.CreateReminderRequest;
import com.sayarti.backend.reminder.dto.ReminderResponse;
import com.sayarti.backend.reminder.dto.UpdateReminderRequest;
import com.sayarti.backend.reminder.entity.Reminder;
import com.sayarti.backend.reminder.entity.ReminderTriggerType;
import com.sayarti.backend.reminder.repository.ReminderRepository;
import com.sayarti.backend.security.jwt.AuthenticatedUser;
import com.sayarti.backend.vehicle.repository.VehicleRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReminderService {
    private final ReminderRepository reminders;
    private final VehicleRepository vehicles;

    public ReminderService(ReminderRepository reminders, VehicleRepository vehicles) {
        this.reminders = reminders;
        this.vehicles = vehicles;
    }

    @Transactional
    public ReminderResponse create(AuthenticatedUser user, UUID vehicleId,
            CreateReminderRequest request) {
        ownedVehicle(user, vehicleId);
        validateTargets(request.triggerType(), request.targetDate(), request.targetMileage());
        Reminder reminder = new Reminder(vehicleId, request.category(), request.title(),
                request.description(), request.triggerType(), request.targetDate(),
                request.targetMileage());
        return ReminderResponse.from(reminders.save(reminder));
    }

    @Transactional(readOnly = true)
    public List<ReminderResponse> list(AuthenticatedUser user, UUID vehicleId) {
        ownedVehicle(user, vehicleId);
        return reminders.findAllByVehicleIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(vehicleId)
                .stream().map(ReminderResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ReminderResponse get(AuthenticatedUser user, UUID vehicleId, UUID reminderId) {
        ownedVehicle(user, vehicleId);
        return ReminderResponse.from(activeReminder(vehicleId, reminderId));
    }

    @Transactional
    public ReminderResponse update(AuthenticatedUser user, UUID vehicleId, UUID reminderId,
            UpdateReminderRequest request) {
        ownedVehicle(user, vehicleId);
        Reminder reminder = activeReminder(vehicleId, reminderId);
        ReminderTriggerType trigger = value(request.triggerType(), reminder.getTriggerType());
        boolean triggerChanged = request.triggerType() != null
                && request.triggerType() != reminder.getTriggerType();
        Instant targetDate = triggerChanged ? request.targetDate()
                : value(request.targetDate(), reminder.getTargetDate());
        Long targetMileage = triggerChanged ? request.targetMileage()
                : value(request.targetMileage(), reminder.getTargetMileage());
        validateTargets(trigger, targetDate, targetMileage);
        reminder.update(value(request.category(), reminder.getCategory()),
                value(request.title(), reminder.getTitle()),
                value(request.description(), reminder.getDescription()), trigger, targetDate,
                targetMileage);
        return ReminderResponse.from(reminder);
    }

    @Transactional
    public ReminderResponse complete(AuthenticatedUser user, UUID vehicleId, UUID reminderId) {
        ownedVehicle(user, vehicleId);
        Reminder reminder = activeReminder(vehicleId, reminderId);
        reminder.complete();
        return ReminderResponse.from(reminder);
    }

    @Transactional
    public void delete(AuthenticatedUser user, UUID vehicleId, UUID reminderId) {
        ownedVehicle(user, vehicleId);
        activeReminder(vehicleId, reminderId).delete();
    }

    private void validateTargets(ReminderTriggerType trigger, Instant date, Long mileage) {
        boolean invalidDate = trigger == ReminderTriggerType.DATE
                && (date == null || mileage != null);
        boolean invalidMileage = trigger == ReminderTriggerType.MILEAGE
                && (mileage == null || mileage < 0 || date != null);
        if (invalidDate || invalidMileage) {
            throw new BusinessValidationException(ErrorCode.INVALID_REMINDER,
                    "DATE reminders require only targetDate; MILEAGE reminders require only "
                            + "a non-negative targetMileage");
        }
    }

    private void ownedVehicle(AuthenticatedUser user, UUID vehicleId) {
        vehicles.findByIdAndUserIdAndDeletedAtIsNull(vehicleId, user.id())
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.VEHICLE_NOT_FOUND, "Vehicle not found"));
    }

    private Reminder activeReminder(UUID vehicleId, UUID reminderId) {
        return reminders.findByIdAndVehicleIdAndDeletedAtIsNull(reminderId, vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.REMINDER_NOT_FOUND, "Reminder not found"));
    }

    private <T> T value(T proposed, T existing) { return proposed == null ? existing : proposed; }
}
