package com.sayarti.backend.fuel.repository;

import com.sayarti.backend.fuel.entity.FuelRecord;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

public interface FuelRecordRepository extends JpaRepository<FuelRecord, UUID> {
    List<FuelRecord> findAllByVehicleIdAndDeletedAtIsNullOrderByFilledAtDescCreatedAtDesc(
            UUID vehicleId);
    Optional<FuelRecord> findByIdAndVehicleIdAndDeletedAtIsNull(UUID id, UUID vehicleId);
    List<FuelRecord> findAllByVehicleIdAndDeletedAtIsNull(UUID vehicleId);
    List<FuelRecord> findByVehicleIdAndDeletedAtIsNullOrderByFilledAtDescIdDesc(
            UUID vehicleId, Pageable pageable);
}
