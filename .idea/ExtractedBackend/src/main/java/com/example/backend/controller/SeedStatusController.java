package com.example.backend.controller;

import com.example.backend.repository.VehicleRepository;
import com.example.backend.repository.VehicleTypeRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class SeedStatusController {

    private final VehicleRepository vehicleRepository;
    private final VehicleTypeRepository vehicleTypeRepository;

    public SeedStatusController(VehicleRepository vehicleRepository, VehicleTypeRepository vehicleTypeRepository) {
        this.vehicleRepository = vehicleRepository;
        this.vehicleTypeRepository = vehicleTypeRepository;
    }

    @GetMapping("/seed-status")
    public Map<String, Object> getSeedStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("vehicleTypes", vehicleTypeRepository.count());
        status.put("vehicles", vehicleRepository.count());
        status.put("message", "Database seeding complete");
        return status;
    }
}
