package com.sayarti.backend.maintenance.repository;

import com.sayarti.backend.maintenance.entity.MaintenanceRecord;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, UUID> {
    List<MaintenanceRecord> findAllByVehicleIdAndDeletedAtIsNullOrderByServiceDateDescCreatedAtDescIdDesc(
            UUID vehicleId);
    Optional<MaintenanceRecord> findByIdAndVehicleIdAndDeletedAtIsNull(UUID id, UUID vehicleId);
    List<MaintenanceRecord> findAllByVehicleIdAndDeletedAtIsNull(UUID vehicleId);
    List<MaintenanceRecord> findByVehicleIdAndDeletedAtIsNullOrderByServiceDateDescIdDesc(
            UUID vehicleId, Pageable pageable);
}
