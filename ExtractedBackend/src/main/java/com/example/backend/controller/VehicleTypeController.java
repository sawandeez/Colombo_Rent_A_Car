package com.example.backend.controller;

import com.example.backend.dto.VehicleTypeDto;
import com.example.backend.service.VehicleTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vehicle-types")
@RequiredArgsConstructor
public class VehicleTypeController {

    private final VehicleTypeService vehicleTypeService;

    @GetMapping
    public ResponseEntity<List<VehicleTypeDto>> getAllTypes() {
        return ResponseEntity.ok(vehicleTypeService.getAllTypes());
    }
}

