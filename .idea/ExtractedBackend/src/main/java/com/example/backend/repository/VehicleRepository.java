package com.example.backend.repository;

import com.example.backend.model.Vehicle;
import com.example.backend.model.VehicleAvailabilityStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface VehicleRepository extends MongoRepository<Vehicle, String> {
    List<Vehicle> findByIsAvailableTrueAndIsUnderMaintenanceFalseAndIsAdminHeldFalse();

    List<Vehicle> findByAvailabilityStatus(VehicleAvailabilityStatus status);

    List<Vehicle> findByTypeAndAvailabilityStatus(String type, VehicleAvailabilityStatus status);

    List<Vehicle> findByVehicleTypeIdAndAvailabilityStatus(String vehicleTypeId, VehicleAvailabilityStatus status);

    List<Vehicle> findByAvailabilityStatusIsNull();

    List<Vehicle> findByType(String type);

    List<Vehicle> findByTypeAndIsAvailableTrueAndIsUnderMaintenanceFalseAndIsAdminHeldFalse(String type);

    List<Vehicle> findByVehicleTypeId(String vehicleTypeId);
}
