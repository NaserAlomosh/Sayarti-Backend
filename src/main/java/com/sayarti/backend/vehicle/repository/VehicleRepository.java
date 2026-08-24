package com.sayarti.backend.vehicle.repository;

import com.sayarti.backend.vehicle.entity.Vehicle;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID>, JpaSpecificationExecutor<Vehicle> {
    List<Vehicle> findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID userId);
    Optional<Vehicle> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);
    Optional<Vehicle> findByUserIdAndLicensePlate(UUID userId, String licensePlate);
}
