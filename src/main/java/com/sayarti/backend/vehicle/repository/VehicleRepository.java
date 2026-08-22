package com.sayarti.backend.vehicle.repository;

import com.sayarti.backend.vehicle.entity.Vehicle;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {
    List<Vehicle> findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID userId);
    Optional<Vehicle> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);
}
