package com.example.backend.repository;

import com.example.backend.model.Vehicle;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface VehicleRepository extends MongoRepository<Vehicle, String> {
    // find vehicles by type for filtering
    List<Vehicle> findByType(String type);
}
