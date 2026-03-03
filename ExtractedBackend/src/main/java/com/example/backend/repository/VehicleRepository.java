package com.example.backend.repository;

import com.example.backend.model.Vehicle;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface VehicleRepository extends MongoRepository<Vehicle, String> {
    // find vehicles by type id for filtering
    List<Vehicle> findByVehicleTypeId(String vehicleTypeId);
}
