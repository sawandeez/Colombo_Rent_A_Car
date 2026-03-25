package com.example.backend.repository;

import com.example.backend.model.VehicleType;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface VehicleTypeRepository extends MongoRepository<VehicleType, String> {
}
