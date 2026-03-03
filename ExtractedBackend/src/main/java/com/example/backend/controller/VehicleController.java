package com.example.backend.controller;

import com.example.backend.dto.VehicleTypeDto;
import com.example.backend.repository.VehicleRepository;
import com.example.backend.service.VehicleTypeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@lombok.extern.slf4j.Slf4j
public class VehicleController {

    private final VehicleRepository vehicleRepository;
    private final VehicleTypeService vehicleTypeService;
    private final com.example.backend.service.VehicleService vehicleService;

    public VehicleController(VehicleRepository vehicleRepository, VehicleTypeService vehicleTypeService,
                             com.example.backend.service.VehicleService vehicleService) {
        this.vehicleRepository = vehicleRepository;
        this.vehicleTypeService = vehicleTypeService;
        this.vehicleService = vehicleService;
    }

    @GetMapping("/vehicles")
    public ResponseEntity<?> getAllVehicles(@org.springframework.web.bind.annotation.RequestParam(value = "typeId", required = false) String typeId) {
        try {
            if (typeId != null) {
                List<com.example.backend.dto.VehicleSummaryDto> filtered = vehicleService.getVehiclesByType(typeId);
                return ResponseEntity.ok(filtered);
            }
            return ResponseEntity.ok(vehicleRepository.findAll());
        } catch (IllegalArgumentException iae) {
            log.warn("Bad request in getAllVehicles: {}", iae.getMessage());
            return ResponseEntity.badRequest().body(iae.getMessage());
        } catch (Exception e) {
            log.error("Failed to fetch vehicles", e);
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/vehicle-types")
    public ResponseEntity<List<VehicleTypeDto>> getAllVehicleTypes() {
        try {
            List<VehicleTypeDto> types = vehicleTypeService.getAllTypes();
            return ResponseEntity.ok(types);
        } catch (Exception e) {
            log.error("Failed to fetch vehicle types", e);
            return ResponseEntity.status(500).build();
        }
    }
}
